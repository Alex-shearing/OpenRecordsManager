package com.openrecordsmanager.api.location;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Locale;

/**
 * Discriminator / constraint kind for a {@code Location} (user, group, or any).
 */
@Schema(name = "LocationKind", enumAsRef = true)
public enum LocationKind {
    USER,
    GROUP,
    ANY;

    @JsonValue
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    @JsonCreator
    public static LocationKind fromString(String value) {
        return LocationKind.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    public boolean matches(LocationKind actual) {
        return this == ANY || this == actual;
    }
}
