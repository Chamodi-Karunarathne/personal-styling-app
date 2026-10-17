package com.chamodi.styling.profile.dto;

import java.time.Instant;

import com.chamodi.styling.profile.model.OnboardingStatus;
import com.chamodi.styling.profile.model.StepCode;

public record ProfileResponse(
		Long version,
		OnboardingStatus onboardingStatus,
		StepCode lastVisitedStep,
		ProfilePreferences preferences,
		Instant completedAt,
		Instant updatedAt,
		boolean appearanceStepPending) {

	public ProfileResponse(Long version, OnboardingStatus status, StepCode step, ProfilePreferences preferences,
			Instant completedAt, Instant updatedAt) {
		this(version, status, step, preferences, completedAt, updatedAt, false);
	}

	public static ProfileResponse notStarted() {
		return new ProfileResponse(null, OnboardingStatus.NOT_STARTED, StepCode.STYLE,
				ProfilePreferences.unanswered(), null, null);
	}

	@Override
	public String toString() {
		return "ProfileResponse[REDACTED]";
	}
}
