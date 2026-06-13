package com.openrecordsmanager.resources;

import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Pattern;

public class ResourceIdentifier implements Serializable {
    private static final Pattern VALID_IDENTIFIER = Pattern.compile("^[a-z_]+$");

    public final String source;
    public final String item;

    public ResourceIdentifier(String source, String item) {
        this.source = validateIdentifier(source);
        this.item = validateIdentifier(item);
    }

    public static ResourceIdentifier valueOf(String string) {
        String[] parts = string.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException(String.format("Resource identifier '%s' is invalid. Could not split correctly", string));
        }

        return new ResourceIdentifier(parts[0], parts[1]);
    }

    @Override
    @JsonValue
    public String toString() {
        return this.source + ":" + this.item;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ResourceIdentifier that = (ResourceIdentifier) o;
        return Objects.equals(source, that.source) && Objects.equals(item, that.item);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, item);
    }

    private static String validateIdentifier(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException(String.format("Invalid identifier '%s' must have length", input));
        }

        if (!VALID_IDENTIFIER.matcher(input).matches()) {
            throw new IllegalArgumentException(String.format("Invalid identifier '%s can only container lower-case letters or underscore", input));
        }

        return input;
    }

}
