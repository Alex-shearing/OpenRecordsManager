package com.openrecordsmanager.location.relationship;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum RelationshipDirection {
    OUTGOING,
    INCOMING;

    @JsonValue
    public String key() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    @JsonCreator
    public static RelationshipDirection fromString(String value) {
        return RelationshipDirection.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
