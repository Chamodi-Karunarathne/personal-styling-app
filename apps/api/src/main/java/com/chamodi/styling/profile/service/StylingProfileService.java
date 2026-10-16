package com.chamodi.styling.profile.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chamodi.styling.auth.entity.User;
import com.chamodi.styling.auth.repository.UserRepository;
import com.chamodi.styling.profile.dto.PreferenceCatalogResponse;
import com.chamodi.styling.profile.dto.ProfileResponse;
import com.chamodi.styling.profile.dto.ProfileWriteRequest;
import com.chamodi.styling.profile.entity.StylingProfile;
import com.chamodi.styling.profile.exception.ProfileConflictException;
import com.chamodi.styling.profile.exception.ProfileValidationException;
import com.chamodi.styling.profile.model.OnboardingStatus;
import com.chamodi.styling.profile.model.WriteIntent;
import com.chamodi.styling.profile.repository.StylingProfileRepository;

@Service
public class StylingProfileService {

	private final StylingProfileRepository profiles;
	private final UserRepository users;
	private final PreferenceCatalog catalog;

	public StylingProfileService(StylingProfileRepository profiles, UserRepository users, PreferenceCatalog catalog) {
		this.profiles = profiles;
		this.users = users;
		this.catalog = catalog;
	}

	public PreferenceCatalogResponse options() {
		return catalog.response();
	}

	@Transactional(readOnly = true)
	public ProfileResponse get(Long ownerId) {
		return profiles.findById(ownerId).map(this::response).orElseGet(ProfileResponse::notStarted);
	}

	@Transactional
	public ProfileResponse save(Long ownerId, ProfileWriteRequest request) {
		validateShape(request);
		// Build and validate the full replacement before touching any managed collection.
		PreferenceCatalog.PreparedPreferences prepared = catalog.prepare(request.preferences());
		StylingProfile profile = profiles.findById(ownerId).orElse(null);
		if (profile == null ? request.version() != null : !Objects.equals(profile.getVersion(), request.version())) {
			throw new ProfileConflictException();
		}
		if (profile != null && profile.getOnboardingStatus() == OnboardingStatus.COMPLETED && request.intent() == WriteIntent.SKIP) {
			throw new ProfileValidationException(Map.of("intent", "Completed onboarding cannot be skipped"));
		}
		OnboardingStatus status = switch (request.intent()) {
			case COMPLETE -> OnboardingStatus.COMPLETED;
			case SKIP -> OnboardingStatus.SKIPPED;
			case SAVE -> profile != null && profile.getOnboardingStatus() == OnboardingStatus.COMPLETED
					? OnboardingStatus.COMPLETED : OnboardingStatus.IN_PROGRESS;
		};
		if (profile == null) {
			User owner = users.findById(ownerId).orElseThrow(() -> new IllegalStateException("Authenticated account is unavailable"));
			profile = new StylingProfile(owner);
		}
		profile.apply(status, request.lastVisitedStep(), prepared.answered(), prepared.choices(), Instant.now());
		try {
			return response(profiles.saveAndFlush(profile));
		} catch (OptimisticLockingFailureException exception) {
			throw new ProfileConflictException();
		} catch (DataIntegrityViolationException exception) {
			if (isConcurrentCreation(exception)) {
				throw new ProfileConflictException();
			}
			throw exception;
		}
	}

	private void validateShape(ProfileWriteRequest request) {
		Map<String, String> errors = new LinkedHashMap<>();
		if (request == null) {
			throw new ProfileValidationException(Map.of("preferences", "Preferences are required"));
		}
		if (request.version() != null && request.version() < 0) {
			errors.put("version", "Version must be nonnegative");
		}
		if (request.intent() == null) {
			errors.put("intent", "Choose a save action");
		}
		if (request.lastVisitedStep() == null) {
			errors.put("lastVisitedStep", "Choose a valid step");
		}
		if (request.preferences() == null) {
			errors.put("preferences", "Preferences are required");
		}
		if (!errors.isEmpty()) {
			throw new ProfileValidationException(errors);
		}
	}

	private ProfileResponse response(StylingProfile profile) {
		return new ProfileResponse(profile.getVersion(), profile.getOnboardingStatus(), profile.getLastVisitedStep(),
				catalog.preferences(profile), profile.getCompletedAt(), profile.getUpdatedAt());
	}

	private boolean isConcurrentCreation(DataIntegrityViolationException exception) {
		for (Throwable cause = exception.getCause(); cause != null; cause = cause.getCause()) {
			if (cause instanceof ConstraintViolationException violation) {
				return "23505".equals(violation.getSQLState())
						&& "styling_profiles_pkey".equalsIgnoreCase(violation.getConstraintName());
			}
		}
		return false;
	}
}
