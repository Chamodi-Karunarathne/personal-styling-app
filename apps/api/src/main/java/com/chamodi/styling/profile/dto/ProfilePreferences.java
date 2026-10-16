package com.chamodi.styling.profile.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProfilePreferences(
		@JsonProperty(value = "styles", required = true)
		@Size(max = 7, message = "Choose each option only once")
		List<@NotBlank(message = "Choose a supported option") @Size(max = 40) String> styles,
		@JsonProperty(value = "preferredColors", required = true)
		@Size(max = 13, message = "Choose each option only once")
		List<@NotBlank(message = "Choose a supported option") @Size(max = 40) String> preferredColors,
		@JsonProperty(value = "dislikedColors", required = true)
		@Size(max = 13, message = "Choose each option only once")
		List<@NotBlank(message = "Choose a supported option") @Size(max = 40) String> dislikedColors,
		@JsonProperty(value = "fits", required = true)
		@Size(max = 3, message = "Choose each option only once")
		List<@NotBlank(message = "Choose a supported option") @Size(max = 40) String> fits,
		@JsonProperty(value = "footwear", required = true)
		@Size(max = 5, message = "Choose each option only once")
		List<@NotBlank(message = "Choose a supported option") @Size(max = 40) String> footwear,
		@JsonProperty(value = "comfort", required = true)
		@Size(max = 3, message = "Choose each option only once")
		List<@NotBlank(message = "Choose a supported option") @Size(max = 40) String> comfort,
		@JsonProperty(value = "coverage", required = true)
		@Size(max = 3, message = "Choose each option only once")
		List<@NotNull(message = "Choose a supported option") @Valid CoverageChoice> coverage,
		@JsonProperty(value = "commonOccasions", required = true)
		@Size(max = 4, message = "Choose each option only once")
		List<@NotBlank(message = "Choose a supported option") @Size(max = 40) String> commonOccasions,
		@JsonProperty(value = "activities", required = true)
		@Size(max = 3, message = "Choose each option only once")
		List<@NotBlank(message = "Choose a supported option") @Size(max = 40) String> activities,
		@JsonProperty(value = "transportModes", required = true)
		@Size(max = 4, message = "Choose each option only once")
		List<@NotBlank(message = "Choose a supported option") @Size(max = 40) String> transportModes) {

	public static ProfilePreferences unanswered() {
		return new ProfilePreferences(null, null, null, null, null, null, null, null, null, null);
	}

	@JsonAnySetter
	public void rejectUnknownCategory(String ignoredName, Object ignoredValue) {
		throw new IllegalArgumentException("Unsupported preference category");
	}

	@Override
	public String toString() {
		return "ProfilePreferences[REDACTED]";
	}
}
