package com.openrecordsmanager.api;

import java.time.Instant;
import java.util.Map;

public record ValidationErrorResponse(
        boolean success,
        String errorCode,
        Map<String, String> fieldErrors,
        Instant timestamp
) {
    public static ValidationErrorResponse validationFailed(Map<String, String> fieldErrors) {
        return new ValidationErrorResponse(false, "validation_failed", Map.copyOf(fieldErrors), Instant.now());
    }
}
