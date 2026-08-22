package com.openrecordsmanager.api.errors;

import java.util.Map;

public class InputValidationException extends RuntimeException {
    private final Map<String, String> fieldErrors;

    public InputValidationException(Map<String, String> fieldErrors) {
        super("Input validation failed");
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public Map<String, String> getFieldErrors() {
        return this.fieldErrors;
    }
}
