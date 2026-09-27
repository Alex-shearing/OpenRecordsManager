package com.openrecordsmanager.api.search;

import com.openrecordsmanager.api.ResourceIdentifier;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

/**
 * A single filter on an object property or plugin-provided search field.
 *
 * <p>{@link #value()} is omitted for {@link SearchOperator#IS_NULL} / {@link SearchOperator#IS_NOT_NULL}.
 * For {@link SearchOperator#IN}, {@link SearchOperator#NOT_IN}, and {@link SearchOperator#BETWEEN},
 * value is a JSON array.
 */
public record SearchClause(
        ResourceIdentifier field,
        SearchOperator op,
        @Nullable JsonNode value
) {
}
