package com.openrecordsmanager.api.errors;

import java.util.List;
import java.util.Map;

/**
 * Base runtime exception that carries a machine-readable API error code
 * (catalog key is {@code error.<code>} on the client), optional interpolation args,
 * and optional per-field errors.
 */
public class ApiException extends RuntimeException {
    private final ApiError error;
    private final Map<String, ApiError> fieldErrors;

    public ApiException(String code) {
        this(code, List.of(), Map.of());
    }

    public ApiException(String code, String... args) {
        this(code, List.of(args), Map.of());
    }

    public ApiException(String code, List<String> args) {
        this(code, args, Map.of());
    }

    public ApiException(String code, List<String> args, Map<String, ApiError> fieldErrors) {
        super(code);
        if (code.isBlank()) {
            throw new IllegalArgumentException("error code is required");
        }
        this.error = new ApiError(code, List.copyOf(args));
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public ApiError getError() {
        return this.error;
    }

    public Map<String, ApiError> getFieldErrors() {
        return this.fieldErrors;
    }

    /**
     * Top-level code {@code validation_failed} with one field error.
     */
    public static ApiException validationFailed(String field, String code) {
        return new ApiException("validation_failed", List.of(), Map.of(field, ApiError.of(code)));
    }

    /**
     * Top-level code {@code validation_failed} with one field error and args.
     */
    public static ApiException validationFailed(String field, String code, String... args) {
        return new ApiException("validation_failed", List.of(), Map.of(field, ApiError.of(code, args)));
    }

    /**
     * Top-level {@code validation_failed} with a field map (may be empty).
     */
    public static ApiException validationFailed(Map<String, ApiError> fieldErrors) {
        return new ApiException("validation_failed", List.of(), fieldErrors);
    }
}
