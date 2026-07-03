package com.openrecordsmanager.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.jspecify.annotations.NonNull;

import java.io.Serializable;
import java.util.regex.Pattern;

public record ResourceIdentifier(String source, String item) implements Serializable {
    private static final Pattern VALID_IDENTIFIER = Pattern.compile("^[a-z0-9_.-]+$");

    public ResourceIdentifier(String source, String item) {
        this.source = validateIdentifier(source);
        this.item = validateIdentifier(item);
    }

    public ResourceIdentifier(ResourceIdentifier identifier) {
        this(identifier.source, identifier.item);
    }

    @JsonCreator
    public static ResourceIdentifier valueOf(String identifier) {
        String[] parts = identifier.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException(String.format("Resource identifier '%s' is invalid. Could not split correctly", identifier));
        }

        return new ResourceIdentifier(parts[0], parts[1]);
    }

    @Override
    @JsonValue
    @NonNull
    public String toString() {
        return this.source + ":" + this.item;
    }

    private static String validateIdentifier(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException(String.format("Invalid identifier '%s' must have length", input));
        }

        if (!VALID_IDENTIFIER.matcher(input).matches()) {
            throw new IllegalArgumentException(String.format("Invalid identifier '%s', contains invalid characters", input));
        }

        return input;
    }

}
