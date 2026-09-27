package com.openrecordsmanager.api.search;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

/**
 * Which {@code ObjectPropertyHolder} kind a {@link SearchFieldProvider} applies to.
 */
public enum SearchFieldTarget {
    RECORD,
    USER;

    @JsonCreator
    public static SearchFieldTarget fromString(String value) {
        return SearchFieldTarget.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
