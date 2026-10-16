package com.chamodi.styling.profile.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import com.chamodi.styling.auth.entity.User;
import com.chamodi.styling.auth.repository.UserRepository;
import com.chamodi.styling.profile.dto.ProfilePreferences;
import com.chamodi.styling.profile.dto.ProfileWriteRequest;
import com.chamodi.styling.profile.entity.StylingProfile;
import com.chamodi.styling.profile.exception.ProfileConflictException;
import com.chamodi.styling.profile.exception.ProfileValidationException;
import com.chamodi.styling.profile.model.OnboardingStatus;
import com.chamodi.styling.profile.model.StepCode;
import com.chamodi.styling.profile.model.WriteIntent;
import com.chamodi.styling.profile.repository.StylingProfileRepository;

@ExtendWith(MockitoExtension.class)
class StylingProfileServiceTests {

	@Mock private StylingProfileRepository profiles;
	@Mock private UserRepository users;
	private final PreferenceCatalog catalog = new PreferenceCatalog();
	private StylingProfileService service;

	@BeforeEach
	void setUp() {
		service = new StylingProfileService(profiles, users, catalog);
	}

	@Test
	void missingProfileReturnsVirtualNotStartedWithoutCreatingAnything() {
		when(profiles.findById(7L)).thenReturn(Optional.empty());
		var result = service.get(7L);
		assertEquals(OnboardingStatus.NOT_STARTED, result.onboardingStatus());
		assertEquals(StepCode.STYLE, result.lastVisitedStep());
		assertNull(result.version());
		assertNull(result.preferences().styles());
		verify(profiles, never()).saveAndFlush(any());
		verifyNoInteractions(users);
	}

	@Test
	void catalogDoesNotQueryAccountOrProfilePersistence() {
		assertEquals(10, service.options().groups().size());
		verifyNoInteractions(profiles, users);
	}

	@Test
	void firstSaveUsesAuthenticatedOwnerAndPersistsDraftWithoutInventingPreferences() {
		when(profiles.findById(7L)).thenReturn(Optional.empty());
		User owner = owner();
		when(users.findById(7L)).thenReturn(Optional.of(owner));
		stubSave();
		var result = service.save(7L, request(null, WriteIntent.SAVE, preferences(List.of(), null)));
		assertEquals(OnboardingStatus.IN_PROGRESS, result.onboardingStatus());
		assertEquals(List.of(), result.preferences().styles());
		assertNull(result.preferences().preferredColors());
		assertNotNull(result.updatedAt());
		ArgumentCaptor<StylingProfile> saved = ArgumentCaptor.forClass(StylingProfile.class);
		verify(profiles).saveAndFlush(saved.capture());
		assertEquals(7L, saved.getValue().getUserId());
	}

	@Test
	void allUnansweredPreferencesCanBeExplicitlyCompleted() {
		when(profiles.findById(7L)).thenReturn(Optional.empty());
		when(users.findById(7L)).thenReturn(Optional.of(owner()));
		stubSave();
		var result = service.save(7L, request(null, WriteIntent.COMPLETE, ProfilePreferences.unanswered()));
		assertEquals(OnboardingStatus.COMPLETED, result.onboardingStatus());
		assertNotNull(result.completedAt());
		assertNull(result.preferences().coverage());
	}

	@Test
	void skipPreservesSelectionsAndSaveResumesSkippedProfile() {
		StylingProfile profile = existing(OnboardingStatus.IN_PROGRESS);
		when(profiles.findById(7L)).thenReturn(Optional.of(profile));
		stubSave();
		var skipped = service.save(7L, request(3L, WriteIntent.SKIP, preferences(List.of("CASUAL"), null)));
		assertEquals(OnboardingStatus.SKIPPED, skipped.onboardingStatus());
		assertEquals(List.of("CASUAL"), skipped.preferences().styles());
		var resumed = service.save(7L, request(skipped.version(), WriteIntent.SAVE, skipped.preferences()));
		assertEquals(OnboardingStatus.IN_PROGRESS, resumed.onboardingStatus());
		assertEquals(List.of("CASUAL"), resumed.preferences().styles());
	}

	@Test
	void editingCompletedProfilePreservesCompletionAndItsFirstTimestamp() {
		StylingProfile profile = existing(OnboardingStatus.COMPLETED);
		Instant firstCompletion = profile.getCompletedAt();
		when(profiles.findById(7L)).thenReturn(Optional.of(profile));
		stubSave();
		var saved = service.save(7L, request(3L, WriteIntent.SAVE, preferences(List.of("MINIMAL"), null)));
		assertEquals(OnboardingStatus.COMPLETED, saved.onboardingStatus());
		assertEquals(firstCompletion, saved.completedAt());
		var completedAgain = service.save(7L, request(saved.version(), WriteIntent.COMPLETE, saved.preferences()));
		assertEquals(firstCompletion, completedAgain.completedAt());
	}

	@Test
	void completedOnboardingCannotBeDowngradedThroughSkip() {
		StylingProfile profile = existing(OnboardingStatus.COMPLETED);
		when(profiles.findById(7L)).thenReturn(Optional.of(profile));
		var exception = assertThrows(ProfileValidationException.class,
				() -> service.save(7L, request(3L, WriteIntent.SKIP, ProfilePreferences.unanswered())));
		assertTrue(exception.getFieldErrors().containsKey("intent"));
		assertEquals(OnboardingStatus.COMPLETED, profile.getOnboardingStatus());
		verify(profiles, never()).saveAndFlush(any());
	}

	@Test
	void invalidChoicesAreRejectedBeforeReadingOrMutatingPersistence() {
		var exception = assertThrows(ProfileValidationException.class,
				() -> service.save(7L, request(null, WriteIntent.SAVE, preferences(List.of("UNKNOWN"), null))));
		assertTrue(exception.getFieldErrors().containsKey("preferences.styles"));
		verifyNoInteractions(profiles, users);
	}

	@Test
	void malformedSnapshotIsRejectedWithoutPersistenceAccess() {
		var exception = assertThrows(ProfileValidationException.class,
				() -> service.save(7L, new ProfileWriteRequest(-1L, null, null, null)));
		assertEquals(4, exception.getFieldErrors().size());
		verifyNoInteractions(profiles, users);
	}

	@Test
	void staleVersionLeavesManagedProfileUnchanged() {
		StylingProfile profile = existing(OnboardingStatus.IN_PROGRESS);
		when(profiles.findById(7L)).thenReturn(Optional.of(profile));
		Instant savedAt = profile.getUpdatedAt();
		assertThrows(ProfileConflictException.class,
				() -> service.save(7L, request(2L, WriteIntent.SAVE, preferences(List.of("MINIMAL"), null))));
		assertEquals(savedAt, profile.getUpdatedAt());
		assertEquals(List.of("CASUAL"), catalog.preferences(profile).styles());
		verify(profiles, never()).saveAndFlush(any());
	}

	@Test
	void nullVersionCannotOverwriteAnExistingProfile() {
		when(profiles.findById(7L)).thenReturn(Optional.of(existing(OnboardingStatus.IN_PROGRESS)));
		assertThrows(ProfileConflictException.class,
				() -> service.save(7L, request(null, WriteIntent.SAVE, ProfilePreferences.unanswered())));
		verify(profiles, never()).saveAndFlush(any());
	}

	@Test
	void nonnullVersionCannotRecreateAnAbsentProfile() {
		when(profiles.findById(7L)).thenReturn(Optional.empty());
		assertThrows(ProfileConflictException.class,
				() -> service.save(7L, request(2L, WriteIntent.SAVE, ProfilePreferences.unanswered())));
		verify(profiles, never()).saveAndFlush(any());
		verifyNoInteractions(users);
	}

	@Test
	void translatesActualOptimisticPersistenceFailureToProfileConflict() {
		when(profiles.findById(7L)).thenReturn(Optional.of(existing(OnboardingStatus.IN_PROGRESS)));
		when(profiles.saveAndFlush(any())).thenThrow(new ObjectOptimisticLockingFailureException(StylingProfile.class, 7L));
		assertThrows(ProfileConflictException.class,
				() -> service.save(7L, request(3L, WriteIntent.SAVE, ProfilePreferences.unanswered())));
	}

	@Test
	void translatesOnlyTheProfilePrimaryKeyUniquenessRace() {
		prepareFirstCreateFailure(integrity("23505", "styling_profiles_pkey"));
		assertThrows(ProfileConflictException.class,
				() -> service.save(7L, request(null, WriteIntent.SAVE, ProfilePreferences.unanswered())));
	}

	@Test
	void doesNotMisclassifyOtherUniquenessViolations() {
		var failure = integrity("23505", "uk_profile_choice");
		prepareFirstCreateFailure(failure);
		assertSame(failure, assertThrows(DataIntegrityViolationException.class,
				() -> service.save(7L, request(null, WriteIntent.SAVE, ProfilePreferences.unanswered()))));
	}

	@Test
	void doesNotMisclassifyMissingOwnerForeignKeyAsConcurrentCreation() {
		var failure = integrity("23503", "styling_profiles_pkey");
		prepareFirstCreateFailure(failure);
		assertSame(failure, assertThrows(DataIntegrityViolationException.class,
				() -> service.save(7L, request(null, WriteIntent.SAVE, ProfilePreferences.unanswered()))));
	}

	@Test
	void readsOnlyTheRequestedAuthenticatedOwnerKey() {
		when(profiles.findById(7L)).thenReturn(Optional.of(existing(OnboardingStatus.IN_PROGRESS)));
		assertEquals(List.of("CASUAL"), service.get(7L).preferences().styles());
		verify(profiles).findById(7L);
		verify(profiles, never()).findById(8L);
	}

	private void prepareFirstCreateFailure(RuntimeException failure) {
		when(profiles.findById(7L)).thenReturn(Optional.empty());
		when(users.findById(7L)).thenReturn(Optional.of(owner()));
		when(profiles.saveAndFlush(any())).thenThrow(failure);
	}

	private DataIntegrityViolationException integrity(String state, String constraint) {
		return new DataIntegrityViolationException("Persistence failed",
				new ConstraintViolationException("Constraint failed", new SQLException("Constraint failed", state), constraint));
	}

	private void stubSave() {
		when(profiles.saveAndFlush(any())).thenAnswer(invocation -> {
			StylingProfile profile = invocation.getArgument(0);
			ReflectionTestUtils.setField(profile, "version", profile.getVersion() == null ? 0L : profile.getVersion() + 1);
			return profile;
		});
	}

	private StylingProfile existing(OnboardingStatus status) {
		StylingProfile profile = new StylingProfile(owner());
		var prepared = catalog.prepare(preferences(List.of("CASUAL"), null));
		profile.apply(status, StepCode.STYLE, prepared.answered(), prepared.choices(), Instant.parse("2026-01-01T00:00:00Z"));
		ReflectionTestUtils.setField(profile, "version", 3L);
		return profile;
	}

	private User owner() {
		User user = new User("Profile Test", "profile@example.com", "unused-test-hash");
		ReflectionTestUtils.setField(user, "id", 7L);
		return user;
	}

	private ProfileWriteRequest request(Long version, WriteIntent intent, ProfilePreferences preferences) {
		return new ProfileWriteRequest(version, intent, StepCode.REVIEW, preferences);
	}

	private ProfilePreferences preferences(List<String> styles, List<String> colors) {
		return new ProfilePreferences(styles, colors, null, null, null, null, null, null, null, null);
	}
}
