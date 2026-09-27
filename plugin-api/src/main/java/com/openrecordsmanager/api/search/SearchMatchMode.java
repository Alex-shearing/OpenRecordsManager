package com.openrecordsmanager.api.search;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

/**
 * How multiple filter clauses combine. Default search text ({@code q}) is always expanded
 * as an OR across default fields, then combined with filters using this mode.
 */
public enum SearchMatchMode {
    ALL,
    ANY;

    @JsonCreator
    public static SearchMatchMode fromString(String value) {
        return SearchMatchMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
