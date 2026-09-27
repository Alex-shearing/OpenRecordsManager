package com.openrecordsmanager.api.search;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

/**
 * Comparison operators for {@link SearchClause} property / plugin-field filters.
 */
public enum SearchOperator {
    EQ,
    NEQ,
    GT,
    GTE,
    LT,
    LTE,
    LIKE,
    IN,
    NOT_IN,
    IS_NULL,
    IS_NOT_NULL,
    BETWEEN;

    @JsonCreator
    public static SearchOperator fromString(String value) {
        return SearchOperator.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
