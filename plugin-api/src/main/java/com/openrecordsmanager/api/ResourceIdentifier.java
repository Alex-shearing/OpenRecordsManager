package com.openrecordsmanager.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.openrecordsmanager.api.builtin.BuiltinPlugin;
import com.openrecordsmanager.api.errors.ApiException;

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
            throw new InvalidIdentifierException(identifier);
        }

        return new ResourceIdentifier(parts[0], parts[1]);
    }

    @Override
    @JsonValue
    public String toString() {
        return this.source + ":" + this.item;
    }

    private static String validateIdentifier(String input) {
        if (input.isEmpty()) {
            throw new InvalidIdentifierException(input);
        }

        if (!VALID_IDENTIFIER.matcher(input).matches()) {
            throw new InvalidIdentifierException(input);
        }

        return input;
    }

    public String getTranslationKey(String prefix) {
        return prefix + "." + this.source() + "." + this.item();
    }

    public boolean isBuiltin() {
        return this.source.equals(BuiltinPlugin.BUILTIN_PLUGIN_NAME);
    }

    protected static class InvalidIdentifierException extends ApiException {
        public InvalidIdentifierException(String id) {
            super("invalid_resource_identifier", id);
        }
    }
}
