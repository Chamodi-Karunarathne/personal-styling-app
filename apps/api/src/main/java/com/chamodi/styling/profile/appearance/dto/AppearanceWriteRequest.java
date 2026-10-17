package com.chamodi.styling.profile.appearance.dto;

import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record AppearanceWriteRequest(@JsonProperty(required = true) UUID version,
        @NotNull(message = "Personal details are required") @Valid AppearanceDetails details) {
    @JsonAnySetter public void rejectUnknown(String ignoredName, Object ignoredValue) { throw new IllegalArgumentException("Unsupported personal details field"); }
    @Override public String toString() { return "AppearanceWriteRequest[REDACTED]"; }
}
