package com.openrecordsmanager.api.property;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Locale;

/**
 * Kind of Managed Object (have the universal property model).
 */
@Schema(name = "ManagedObjectKind", enumAsRef = true)
public enum ManagedObjectKind {
    USER,
    GROUP,
    RECORD;

    @JsonValue
    public String key() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    @JsonCreator
    public static ManagedObjectKind fromString(String value) {
        return ManagedObjectKind.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    public boolean isLocation() {
        return this == USER || this == GROUP;
    }
}
