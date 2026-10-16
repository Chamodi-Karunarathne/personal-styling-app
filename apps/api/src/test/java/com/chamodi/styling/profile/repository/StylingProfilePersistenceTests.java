package com.chamodi.styling.profile.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.chamodi.styling.auth.entity.User;
import com.chamodi.styling.auth.repository.UserRepository;
import com.chamodi.styling.profile.dto.CoverageChoice;
import com.chamodi.styling.profile.dto.ProfilePreferences;
import com.chamodi.styling.profile.dto.ProfileWriteRequest;
import com.chamodi.styling.profile.entity.StylingProfile;
import com.chamodi.styling.profile.exception.ProfileConflictException;
import com.chamodi.styling.profile.exception.ProfileValidationException;
import com.chamodi.styling.profile.model.OnboardingStatus;
import com.chamodi.styling.profile.model.PreferenceStrength;
import com.chamodi.styling.profile.model.StepCode;
import com.chamodi.styling.profile.model.WriteIntent;
import com.chamodi.styling.profile.service.PreferenceCatalog;
import com.chamodi.styling.profile.service.StylingProfileService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;

// Explicit opt-in and a loopback-only test database prevent accidental Neon access.
@DataJpaTest(properties = { "spring.jpa.hibernate.ddl-auto=update", "spring.jpa.show-sql=false" })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ StylingProfileService.class, PreferenceCatalog.class })
@EnabledIfEnvironmentVariable(named = "PROFILE_INTEGRATION_TESTS", matches = "true")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class StylingProfilePersistenceTests {

	@Autowired private StylingProfileService service;
	@Autowired private StylingProfileRepository profiles;
	@Autowired private UserRepository users;
	@Autowired private PreferenceCatalog catalog;
	@Autowired private EntityManagerFactory entityManagers;
	@Autowired private PlatformTransactionManager transactions;
	@Autowired private DataSource dataSource;

	@DynamicPropertySource
	static void isolatedDatabase(DynamicPropertyRegistry registry) {
		String url = required("PROFILE_TEST_DB_URL");
		if (!url.matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/phase2_test")) {
			throw new IllegalStateException("Profile integration tests require the dedicated loopback phase2_test database");
		}
		registry.add("spring.datasource.url", () -> url);
		registry.add("spring.datasource.username", () -> required("PROFILE_TEST_DB_USERNAME"));
		registry.add("spring.datasource.password", () -> required("PROFILE_TEST_DB_PASSWORD"));
	}

	private static String required(String name) {
		String value = System.getenv(name);
		if (value == null || value.isEmpty()) {
			throw new IllegalStateException("Missing isolated profile test setting: " + name);
		}
		return value;
	}

	@Test
	void missingReadDoesNotCreateProfileRow() {
		Long id = account();
		assertEquals(OnboardingStatus.NOT_STARTED, service.get(id).onboardingStatus());
		assertFalse(profiles.existsById(id));
	}

	@Test
	void roundTripsUnansweredNoPreferenceAndExplicitCoverageStrength() {
		Long id = account();
		ProfilePreferences choices = new ProfilePreferences(List.of(), null, List.of("RED"), null, null, null,
				List.of(new CoverageChoice("COVER_KNEES", PreferenceStrength.REQUIRED)), null, null, null);
		service.save(id, request(null, WriteIntent.SAVE, choices));
		var loaded = service.get(id);
		assertEquals(List.of(), loaded.preferences().styles());
		assertNull(loaded.preferences().preferredColors());
		assertEquals(List.of("RED"), loaded.preferences().dislikedColors());
		assertEquals(PreferenceStrength.REQUIRED, loaded.preferences().coverage().getFirst().strength());
		assertNotNull(loaded.version());
		new TransactionTemplate(transactions).executeWithoutResult(transaction -> {
			StylingProfile profile = profiles.findById(id).orElseThrow();
			assertFalse(profile.getCreatedAt().isAfter(profile.getUpdatedAt()));
			assertEquals(id, profile.getUserId());
		});
	}

	@Test
	void collectionOnlyAndNoOpSavesAdvanceVersion() {
		Long id = account();
		var first = service.save(id, request(null, WriteIntent.SAVE, styles("CASUAL")));
		var second = service.save(id, request(first.version(), WriteIntent.SAVE, styles("MINIMAL")));
		var third = service.save(id, request(second.version(), WriteIntent.SAVE, second.preferences()));
		assertTrue(second.version() > first.version());
		assertTrue(third.version() > second.version());
		assertEquals(List.of("MINIMAL"), service.get(id).preferences().styles());
	}

	@Test
	void completionSkipResumeAndLaterEditsPersistWithoutLosingFirstCompletion() {
		Long id = account();
		var skipped = service.save(id, request(null, WriteIntent.SKIP, styles("CASUAL")));
		assertEquals(OnboardingStatus.SKIPPED, service.get(id).onboardingStatus());
		var resumed = service.save(id, request(skipped.version(), WriteIntent.SAVE, skipped.preferences()));
		assertEquals(OnboardingStatus.IN_PROGRESS, resumed.onboardingStatus());
		var completed = service.save(id, request(resumed.version(), WriteIntent.COMPLETE, ProfilePreferences.unanswered()));
		assertEquals(OnboardingStatus.COMPLETED, completed.onboardingStatus());
		var edited = service.save(id, request(completed.version(), WriteIntent.SAVE, styles("MINIMAL")));
		assertEquals(OnboardingStatus.COMPLETED, service.get(id).onboardingStatus());
		assertEquals(completed.completedAt(), edited.completedAt());
		assertEquals(completed.completedAt(), service.get(id).completedAt());
		assertThrows(ProfileValidationException.class,
				() -> service.save(id, request(edited.version(), WriteIntent.SKIP, styles("CASUAL"))));
	}

	@Test
	void staleUpdatesDoNotOverwriteTheCommittedProfile() {
		Long id = account();
		var first = service.save(id, request(null, WriteIntent.SAVE, styles("CASUAL")));
		var second = service.save(id, request(first.version(), WriteIntent.SAVE, styles("MINIMAL")));
		assertThrows(ProfileConflictException.class,
				() -> service.save(id, request(first.version(), WriteIntent.SAVE, styles("FORMAL"))));
		assertEquals(second.version(), service.get(id).version());
		assertEquals(List.of("MINIMAL"), service.get(id).preferences().styles());
	}

	@Test
	void rejectsInvalidFullReplacementWithoutPartialWrites() {
		Long id = account();
		var first = service.save(id, request(null, WriteIntent.SAVE, styles("CASUAL")));
		ProfilePreferences invalid = new ProfilePreferences(List.of("MINIMAL"), List.of("NAVY"), List.of("NAVY"),
				null, null, null, null, null, null, null);
		assertThrows(ProfileValidationException.class,
				() -> service.save(id, request(first.version(), WriteIntent.COMPLETE, invalid)));
		var unchanged = service.get(id);
		assertEquals(first.version(), unchanged.version());
		assertEquals(OnboardingStatus.IN_PROGRESS, unchanged.onboardingStatus());
		assertEquals(List.of("CASUAL"), unchanged.preferences().styles());
		assertNull(unchanged.completedAt());
	}

	@Test
	void transactionRollbackRestoresOwnedCollectionsAndStatus() {
		Long id = account();
		var first = service.save(id, request(null, WriteIntent.SAVE, styles("CASUAL")));
		assertThrows(IllegalStateException.class, () -> new TransactionTemplate(transactions).executeWithoutResult(transaction -> {
			StylingProfile profile = profiles.findById(id).orElseThrow();
			var replacement = catalog.prepare(styles("MINIMAL"));
			profile.apply(OnboardingStatus.COMPLETED, StepCode.REVIEW, replacement.answered(), replacement.choices(), Instant.now());
			profiles.saveAndFlush(profile);
			throw new IllegalStateException("Deliberate isolated test rollback");
		}));
		var unchanged = service.get(id);
		assertEquals(first.version(), unchanged.version());
		assertEquals(OnboardingStatus.IN_PROGRESS, unchanged.onboardingStatus());
		assertEquals(List.of("CASUAL"), unchanged.preferences().styles());
	}

	@Test
	void ownerKeysKeepAccountsIsolated() {
		Long first = account();
		Long second = account();
		service.save(first, request(null, WriteIntent.SAVE, styles("CASUAL")));
		service.save(second, request(null, WriteIntent.SAVE, styles("MINIMAL")));
		assertEquals(List.of("CASUAL"), service.get(first).preferences().styles());
		assertEquals(List.of("MINIMAL"), service.get(second).preferences().styles());
	}

	@Test
	void databaseRejectsProfileWithoutAnAccount() throws Exception {
		try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(
				"INSERT INTO styling_profiles (user_id,version,onboarding_status,last_visited_step,created_at,updated_at) "
				+ "VALUES (?,0,'IN_PROGRESS','STYLE',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)")) {
			statement.setLong(1, Long.MAX_VALUE);
			SQLException failure = assertThrows(SQLException.class, statement::executeUpdate);
			assertEquals("23503", failure.getSQLState());
		}
	}

	@Test
	void simultaneousUpdatesAllowOnlyOneCommit() throws Exception {
		Long id = account();
		service.save(id, request(null, WriteIntent.SAVE, styles("CASUAL")));
		assertConcurrentOutcome(id, false);
	}

	@Test
	void simultaneousFirstCreationHasOneProfileAndOneNarrowUniquenessConflict() throws Exception {
		Long id = account();
		assertConcurrentOutcome(id, true);
		assertTrue(profiles.existsById(id));
	}

	private void assertConcurrentOutcome(Long id, boolean create) throws Exception {
		CountDownLatch read = new CountDownLatch(2);
		CountDownLatch write = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			var first = executor.submit(() -> concurrentWrite(id, "MINIMAL", create, read, write));
			var second = executor.submit(() -> concurrentWrite(id, "FORMAL", create, read, write));
			try {
				assertTrue(read.await(10, TimeUnit.SECONDS), "Both isolated transactions must read before either writes");
			} finally {
				write.countDown();
			}
			List<String> outcomes = List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
			assertEquals(1, outcomes.stream().filter("SAVED"::equals).count());
			assertEquals(1, outcomes.stream().filter((create ? "CREATE_CONFLICT" : "VERSION_CONFLICT")::equals).count());
		}
	}

	private String concurrentWrite(Long id, String style, boolean create, CountDownLatch read, CountDownLatch write) throws Exception {
		EntityManager manager = entityManagers.createEntityManager();
		try {
			manager.getTransaction().begin();
			StylingProfile profile = manager.find(StylingProfile.class, id);
			if (create) {
				assertNull(profile);
				profile = new StylingProfile(manager.getReference(User.class, id));
			} else {
				assertNotNull(profile);
			}
			read.countDown();
			if (!write.await(10, TimeUnit.SECONDS)) {
				throw new IllegalStateException("Concurrent test barrier timed out");
			}
			var replacement = catalog.prepare(styles(style));
			profile.apply(OnboardingStatus.IN_PROGRESS, StepCode.REVIEW, replacement.answered(), replacement.choices(), Instant.now());
			if (create) {
				manager.persist(profile);
			}
			manager.flush();
			manager.getTransaction().commit();
			return "SAVED";
		} catch (OptimisticLockException exception) {
			return "VERSION_CONFLICT";
		} catch (PersistenceException exception) {
			for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
				if (cause instanceof ConstraintViolationException violation && "23505".equals(violation.getSQLState())
						&& "styling_profiles_pkey".equalsIgnoreCase(violation.getConstraintName())) {
					return "CREATE_CONFLICT";
				}
			}
			throw exception;
		} finally {
			if (manager.getTransaction().isActive()) {
				manager.getTransaction().rollback();
			}
			manager.close();
		}
	}

	private Long account() {
		return users.saveAndFlush(new User("Profile Test", "profile-" + UUID.randomUUID() + "@example.com", "unused-test-hash")).getId();
	}

	private ProfilePreferences styles(String style) {
		return new ProfilePreferences(List.of(style), null, null, null, null, null, null, null, null, null);
	}

	private ProfileWriteRequest request(Long version, WriteIntent intent, ProfilePreferences preferences) {
		return new ProfileWriteRequest(version, intent, StepCode.REVIEW, preferences);
	}
}
