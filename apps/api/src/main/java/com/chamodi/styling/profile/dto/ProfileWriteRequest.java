package com.chamodi.styling.profile.dto;

import com.chamodi.styling.profile.model.StepCode;
import com.chamodi.styling.profile.model.WriteIntent;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ProfileWriteRequest(
		@JsonProperty(value = "version", required = true)
		@PositiveOrZero(message = "Version must be nonnegative") Long version,
		@NotNull(message = "Choose a save action") WriteIntent intent,
		@NotNull(message = "Choose a valid step") StepCode lastVisitedStep,
		@NotNull(message = "Preferences are required") @Valid ProfilePreferences preferences,
		@JsonProperty(defaultValue = "false") Boolean appearanceStepPending) {

	public ProfileWriteRequest {
		// Jackson 3 rejects absent primitive creator values; retain old request compatibility locally.
		appearanceStepPending = Boolean.TRUE.equals(appearanceStepPending);
	}

	public ProfileWriteRequest(Long version, WriteIntent intent, StepCode lastVisitedStep, ProfilePreferences preferences) {
		this(version, intent, lastVisitedStep, preferences, false);
	}

	@JsonAnySetter
	public void rejectUnknownProperty(String ignoredName, Object ignoredValue) {
		throw new IllegalArgumentException("Unsupported profile field");
	}

	@Override
	public String toString() {
		return "ProfileWriteRequest[REDACTED]";
	}
}
