package com.chamodi.styling.profile.appearance.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AppearanceResponse(UUID version, AppearanceDetails details, Instant updatedAt, Map<String, List<Option>> options) {
    public record Option(String code, String label) {}
    @Override public String toString() { return "AppearanceResponse[REDACTED]"; }
}
