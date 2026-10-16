package com.chamodi.styling.profile.dto;

import com.chamodi.styling.profile.model.PreferenceStrength;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CoverageChoice(
		@NotBlank(message = "Choose a supported option")
		@Size(max = 40, message = "Choose a supported option") String code,
		@NotNull(message = "Choose Preferred or Required") PreferenceStrength strength) {

	@Override
	public String toString() {
		return "CoverageChoice[REDACTED]";
	}
}
