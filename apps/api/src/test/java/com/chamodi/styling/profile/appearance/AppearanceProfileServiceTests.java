package com.chamodi.styling.profile.appearance;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;
import com.chamodi.styling.auth.entity.User;
import com.chamodi.styling.auth.repository.UserRepository;
import com.chamodi.styling.profile.appearance.dto.*;
import com.chamodi.styling.profile.appearance.entity.AppearanceProfile;
import com.chamodi.styling.profile.appearance.exception.AppearanceConflictException;
import com.chamodi.styling.profile.appearance.repository.AppearanceProfileRepository;
import com.chamodi.styling.profile.appearance.service.*;
import com.chamodi.styling.profile.exception.ProfileValidationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

@ExtendWith(MockitoExtension.class)
class AppearanceProfileServiceTests {
    private static final ValidatorFactory VALIDATION = Validation.buildDefaultValidatorFactory();
    @Mock private AppearanceProfileRepository profiles;
    @Mock private UserRepository users;
    private AppearanceProfileService service;
    @BeforeEach void setup() { service = new AppearanceProfileService(profiles, users, new AppearanceCatalog(), VALIDATION.getValidator()); }
    @AfterAll static void closeValidation() { VALIDATION.close(); }
    @Test void missingReadIsEmptyAndNeverCreatesAnAccountOrProfile() {
        when(profiles.findById(7L)).thenReturn(Optional.empty());
        var response = service.get(7L);
        assertNull(response.version()); assertEquals(AppearanceDetails.empty(), response.details()); assertNull(response.updatedAt());
        assertEquals(7, response.options().size()); verifyNoInteractions(users); verify(profiles, never()).saveAndFlush(any());
    }
    @Test void firstExplicitSaveUsesOwnerAndPreservesFalseSeparatelyFromNull() {
        firstSave();
        var saved = service.save(7L, new AppearanceWriteRequest(null, details("180", "  M / UK 12  ", false)));
        assertEquals(new BigDecimal("180"), saved.details().heightCm()); assertEquals("M / UK 12", saved.details().garmentSizeNote());
        assertEquals(false, saved.details().paletteSuggestions()); assertNotNull(saved.version());
        verify(users).findById(7L); verify(profiles).saveAndFlush(any());
    }
    @Test void nullableSnapshotCanClearEveryValueAndRetainsTimestamps() {
        var profile = existing(); var created = profile.getCreatedAt();
        when(profiles.findById(7L)).thenReturn(Optional.of(profile)); when(profiles.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        UUID before = profile.getRevision();
        var saved = service.save(7L, new AppearanceWriteRequest(before, AppearanceDetails.empty()));
        assertEquals(AppearanceDetails.empty(), saved.details()); assertNotEquals(before, saved.version()); assertEquals(created, profile.getCreatedAt());
    }
    @ParameterizedTest @ValueSource(strings = { "49", "261", "170.5", "1E+8" })
    void invalidHeightIsRejectedBeforeAnyPersistence(String value) {
        var error = assertThrows(ProfileValidationException.class, () -> service.save(7L, new AppearanceWriteRequest(null, details(value, null, null))));
        assertTrue(error.getFieldErrors().containsKey("details.heightCm")); verifyNoInteractions(profiles, users);
    }
    @Test void overlongSizingNoteIsRejectedWithoutLoggingItsContents() {
        assertThrows(ProfileValidationException.class, () -> service.save(7L, new AppearanceWriteRequest(null, details(null, "x".repeat(61), null))));
        verifyNoInteractions(profiles, users);
    }
    @Test void unsupportedCodesAreNotNormalizedOrInferred() {
        var input = new AppearanceDetails(null, null, null, null, " warm ", null, null, null, null, null);
        var failure = assertThrows(ProfileValidationException.class, () -> service.save(7L, new AppearanceWriteRequest(null, input)));
        assertEquals("Choose a supported option", failure.getFieldErrors().get("details.skinUndertone")); verifyNoInteractions(profiles, users);
    }
    @Test void absentDetailsCannotReachTheRepository() {
        assertThrows(ProfileValidationException.class, () -> service.save(7L, new AppearanceWriteRequest(null, null))); verifyNoInteractions(profiles, users);
    }
    @Test void missingOrStaleVersionCannotOverwriteExistingData() {
        var profile = existing(); when(profiles.findById(7L)).thenReturn(Optional.of(profile));
        var original = profile.details();
        assertThrows(AppearanceConflictException.class, () -> service.save(7L, new AppearanceWriteRequest(null, AppearanceDetails.empty())));
        assertThrows(AppearanceConflictException.class, () -> service.save(7L, new AppearanceWriteRequest(UUID.randomUUID(), AppearanceDetails.empty())));
        assertEquals(original, profile.details()); verify(profiles, never()).saveAndFlush(any());
    }
    @Test void staleVersionCannotRecreateMissingResource() {
        when(profiles.findById(7L)).thenReturn(Optional.empty());
        assertThrows(AppearanceConflictException.class, () -> service.save(7L, new AppearanceWriteRequest(UUID.randomUUID(), AppearanceDetails.empty())));
        verifyNoInteractions(users); verify(profiles, never()).saveAndFlush(any());
    }
    @Test void deletingAbsentResourceIsIdempotentEvenAfterAnEarlierSuccessfulDelete() {
        when(profiles.findById(7L)).thenReturn(Optional.empty()); service.delete(7L, null); service.delete(7L, UUID.randomUUID());
        verify(profiles, never()).delete(any()); verifyNoInteractions(users);
    }
    @Test void deletionRequiresTheCurrentRevisionAndUsesOnlyTheRequestedOwner() {
        var profile = existing(); when(profiles.findById(7L)).thenReturn(Optional.of(profile));
        assertThrows(AppearanceConflictException.class, () -> service.delete(7L, null));
        assertThrows(AppearanceConflictException.class, () -> service.delete(7L, UUID.randomUUID()));
        verify(profiles, never()).delete(any()); service.delete(7L, profile.getRevision()); verify(profiles).delete(profile); verify(profiles).flush();
        verify(profiles, never()).findById(8L);
    }
    @Test void actualOptimisticSaveFailureIsSafeConflict() {
        firstSave(); when(profiles.saveAndFlush(any())).thenThrow(new ObjectOptimisticLockingFailureException(AppearanceProfile.class, 7L));
        assertThrows(AppearanceConflictException.class, () -> service.save(7L, new AppearanceWriteRequest(null, AppearanceDetails.empty())));
    }
    @Test void onlyTheKnownPrimaryKeyCreationRaceIsTranslated() {
        firstSave();
        when(profiles.saveAndFlush(any())).thenThrow(integrity("appearance_profiles_pkey"));
        assertThrows(AppearanceConflictException.class, () -> service.save(7L, new AppearanceWriteRequest(null, AppearanceDetails.empty())));
        var other = integrity("other_constraint"); doThrow(other).when(profiles).saveAndFlush(any());
        assertSame(other, assertThrows(DataIntegrityViolationException.class, () -> service.save(7L, new AppearanceWriteRequest(null, AppearanceDetails.empty()))));
    }
    @Test void detailsRequestsAndResponsesRedactValues() {
        var input = details("180", "PrivateSizingProbe", null);
        assertFalse(input.toString().contains("PrivateSizingProbe"));
        assertFalse(new AppearanceWriteRequest(null, input).toString().contains("180"));
    }
    private void firstSave() {
        when(profiles.findById(7L)).thenReturn(Optional.empty()); when(users.findById(7L)).thenReturn(Optional.of(owner()));
        lenient().when(profiles.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
    }
    private User owner() { User owner = new User("Synthetic User", "appearance@example.com", "unused-test-hash"); ReflectionTestUtils.setField(owner, "id", 7L); return owner; }
    private AppearanceProfile existing() { var profile = new AppearanceProfile(owner()); profile.apply(details("180", "M", true), java.time.Instant.now()); return profile; }
    private AppearanceDetails details(String height, String note, Boolean palette) { return new AppearanceDetails(height == null ? null : new BigDecimal(height), note, null, null, null, palette, null, null, null, null); }
    private DataIntegrityViolationException integrity(String constraint) { return new DataIntegrityViolationException("private diagnostics", new ConstraintViolationException("private diagnostics", new SQLException("private", "23505"), constraint)); }
}
