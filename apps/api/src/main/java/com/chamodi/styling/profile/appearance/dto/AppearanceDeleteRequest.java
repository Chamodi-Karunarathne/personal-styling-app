package com.chamodi.styling.profile.appearance.dto;

import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public record AppearanceDeleteRequest(@JsonProperty(required = true) UUID version) {
    @JsonAnySetter public void rejectUnknown(String ignoredName, Object ignoredValue) { throw new IllegalArgumentException("Unsupported deletion field"); }
    @Override public String toString() { return "AppearanceDeleteRequest[REDACTED]"; }
}
