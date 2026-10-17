package com.chamodi.styling.profile.appearance.dto;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

public record AppearanceDetails(
        @JsonProperty(required = true) @DecimalMin(value = "50", message = "Height must be between 50 and 260 cm")
        @DecimalMax(value = "260", message = "Height must be between 50 and 260 cm")
        @Digits(integer = 3, fraction = 0, message = "Use whole centimeters") BigDecimal heightCm,
        @JsonProperty(required = true) @Size(max = 60, message = "Sizing note must be at most 60 characters") String garmentSizeNote,
        @JsonProperty(required = true) @Size(max = 30) String bodyProportions,
        @JsonProperty(required = true) @Size(max = 30) String skinTone,
        @JsonProperty(required = true) @Size(max = 30) String skinUndertone,
        @JsonProperty(required = true) Boolean paletteSuggestions,
        @JsonProperty(required = true) @Size(max = 30) String hairColor,
        @JsonProperty(required = true) @Size(max = 30) String hairTexture,
        @JsonProperty(required = true) @Size(max = 30) String hairLength,
        @JsonProperty(required = true) @Size(max = 30) String faceShape) {
    public static AppearanceDetails empty() { return new AppearanceDetails(null, null, null, null, null, null, null, null, null, null); }
    @JsonAnySetter public void rejectUnknown(String ignoredName, Object ignoredValue) { throw new IllegalArgumentException("Unsupported personal details field"); }
    @Override public String toString() { return "AppearanceDetails[REDACTED]"; }
}
