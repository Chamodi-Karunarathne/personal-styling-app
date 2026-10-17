package com.chamodi.styling.profile.appearance;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import com.chamodi.styling.auth.entity.User;
import com.chamodi.styling.auth.repository.UserRepository;
import com.chamodi.styling.profile.appearance.dto.*;
import com.chamodi.styling.profile.appearance.exception.AppearanceConflictException;
import com.chamodi.styling.profile.appearance.repository.AppearanceProfileRepository;
import com.chamodi.styling.profile.appearance.service.*;
import com.chamodi.styling.profile.dto.*;
import com.chamodi.styling.profile.exception.ProfileValidationException;
import com.chamodi.styling.profile.model.*;
import com.chamodi.styling.profile.repository.StylingProfileRepository;
import com.chamodi.styling.profile.service.*;

@DataJpaTest(properties = { "spring.jpa.hibernate.ddl-auto=update", "spring.jpa.show-sql=false" })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ AppearanceProfileService.class, AppearanceCatalog.class, ValidationAutoConfiguration.class, StylingProfileService.class, PreferenceCatalog.class })
@EnabledIfEnvironmentVariable(named = "PROFILE_INTEGRATION_TESTS", matches = "true")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AppearancePersistenceTests {
    @Autowired private AppearanceProfileService service;
    @Autowired private AppearanceProfileRepository appearances;
    @Autowired private UserRepository users;
    @Autowired private StylingProfileService styling;
    @Autowired private StylingProfileRepository stylingProfiles;
    @Autowired private PlatformTransactionManager transactions;
    @DynamicPropertySource static void isolated(DynamicPropertyRegistry registry) {
        String url = System.getenv("PROFILE_TEST_DB_URL");
        if (url == null || !url.matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/phase2_test")) throw new IllegalStateException("Dedicated loopback phase2_test database required");
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> System.getenv("PROFILE_TEST_DB_USERNAME"));
        registry.add("spring.datasource.password", () -> System.getenv("PROFILE_TEST_DB_PASSWORD"));
    }
    @Test void getNeverCreatesAppearanceOrStylingRows() {
        Long id = account(); assertNull(service.get(id).version()); assertFalse(appearances.existsById(id)); assertFalse(stylingProfiles.existsById(id));
    }
    @Test void oldReviewAndCompletedRecordsRemainValidWithoutAnAppearanceRow() {
        Long id = account();
        var legacy = styling.save(id, new ProfileWriteRequest(null, WriteIntent.SAVE, StepCode.REVIEW, ProfilePreferences.unanswered()));
        assertFalse(legacy.appearanceStepPending());
        var personal = styling.save(id, new ProfileWriteRequest(legacy.version(), WriteIntent.SAVE, StepCode.REVIEW, legacy.preferences(), true));
        assertTrue(styling.get(id).appearanceStepPending()); assertNull(service.get(id).version());
        assertThrows(ProfileValidationException.class, () -> styling.save(id, new ProfileWriteRequest(personal.version(), WriteIntent.COMPLETE, StepCode.REVIEW, personal.preferences(), true)));
        var completed = styling.save(id, new ProfileWriteRequest(personal.version(), WriteIntent.COMPLETE, StepCode.REVIEW, personal.preferences()));
        assertEquals(OnboardingStatus.COMPLETED, completed.onboardingStatus()); assertFalse(completed.appearanceStepPending());
        assertNull(service.get(id).version());
        assertEquals(completed, styling.get(id));
    }
    @Test void savesClearsAndDeletesIndependentlyOfStylingAndAccount() {
        Long id = account();
        var style = styling.save(id, new ProfileWriteRequest(null, WriteIntent.COMPLETE, StepCode.REVIEW, ProfilePreferences.unanswered()));
        var first = service.save(id, request(null, "172", false));
        assertEquals(first, service.get(id)); assertEquals(false, first.details().paletteSuggestions());
        var cleared = service.save(id, new AppearanceWriteRequest(first.version(), AppearanceDetails.empty()));
        assertEquals(AppearanceDetails.empty(), service.get(id).details());
        service.delete(id, cleared.version()); service.delete(id, cleared.version());
        assertNull(service.get(id).version()); assertEquals(style, styling.get(id)); assertTrue(users.existsById(id));
    }
    @Test void staleSnapshotsAndDeleteRecreateCannotOverwriteNewerDetails() {
        Long id = account(); var first = service.save(id, request(null, "172", null));
        var second = service.save(id, request(first.version(), "173", true));
        assertNotEquals(first.version(), second.version());
        assertThrows(AppearanceConflictException.class, () -> service.delete(id, first.version()));
        assertThrows(AppearanceConflictException.class, () -> service.save(id, request(first.version(), "174", false)));
        service.delete(id, second.version()); var recreated = service.save(id, request(null, "175", false));
        assertThrows(AppearanceConflictException.class, () -> service.save(id, request(first.version(), "176", true)));
        assertThrows(AppearanceConflictException.class, () -> service.delete(id, second.version()));
        assertEquals(recreated, service.get(id));
    }
    @Test void rejectedWriteAndTransactionRollbackLeaveDetailsUnchanged() {
        Long id = account(); var first = service.save(id, request(null, "172", null));
        assertThrows(ProfileValidationException.class, () -> service.save(id, request(first.version(), "172.5", true)));
        assertEquals(first, service.get(id));
        assertThrows(IllegalStateException.class, () -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
            service.save(id, request(first.version(), "173", true)); throw new IllegalStateException("Deliberate isolated rollback");
        }));
        assertEquals(first, service.get(id));
    }
    @Test void separateAccountsRemainIsolatedAndTimestampsAreStable() {
        Long first = account(); Long second = account(); var saved = service.save(first, request(null, "172", true));
        assertNull(service.get(second).version());
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            var profile = appearances.findById(first).orElseThrow(); assertEquals(first, profile.getUserId());
            assertFalse(profile.getCreatedAt().isAfter(profile.getUpdatedAt()));
        });
        var unchanged = service.save(first, request(saved.version(), "172", true));
        assertNotEquals(saved.version(), unchanged.version()); assertTrue(unchanged.updatedAt().isAfter(saved.updatedAt()));
    }
    @Test void concurrentFirstSavesHaveOneWinnerAndOneSafeConflict() throws Exception { race(account(), null); }
    @Test void concurrentSameVersionUpdatesHaveOneWinnerAndOneSafeConflict() throws Exception {
        Long id = account(); race(id, service.save(id, request(null, "172", true)).version());
    }
    private void race(Long id, UUID version) throws Exception {
        CountDownLatch ready = new CountDownLatch(2); CountDownLatch go = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<String> call = () -> { ready.countDown(); if (!go.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out");
                try { service.save(id, request(version, "172", true)); return "SAVED"; } catch (AppearanceConflictException conflict) { return "CONFLICT"; } };
            var first = executor.submit(call); var second = executor.submit(call);
            assertTrue(ready.await(10, TimeUnit.SECONDS)); go.countDown();
            var outcomes = List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
            assertEquals(1, outcomes.stream().filter("SAVED"::equals).count()); assertEquals(1, outcomes.stream().filter("CONFLICT"::equals).count());
            assertNotNull(service.get(id).version());
        }
    }
    private Long account() { return users.saveAndFlush(new User("Appearance Test", "appearance-" + UUID.randomUUID() + "@example.com", "unused-test-hash")).getId(); }
    private AppearanceWriteRequest request(UUID version, String height, Boolean palette) { return new AppearanceWriteRequest(version, new AppearanceDetails(new BigDecimal(height), "M", null, "MEDIUM", "UNSURE", palette, "BROWN", "WAVY", "SHORT", "UNSURE")); }
}
