package com.openrecordsmanager.search;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.errors.ApiException;
import com.openrecordsmanager.api.search.SearchClause;
import com.openrecordsmanager.api.search.SearchOperator;
import com.openrecordsmanager.api.template.property.PropertyType;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

import java.util.*;

/**
 * Validates search operators against property / field types.
 */
public final class SearchOperatorSupport {
    public static final String OPERATOR_UNSUPPORTED = "search_operator_unsupported";
    public static final String OPERATOR_REQUIRES_VALUE = "search_operator_requires_value";
    public static final String OPERATOR_REQUIRES_ARRAY = "search_operator_requires_array";
    public static final String OPERATOR_REQUIRES_TWO_VALUES = "search_operator_requires_two_values";
    public static final String FIELD_UNSUPPORTED = "search_field_unsupported";
    public static final String TYPE_SCOPE_UNSUPPORTED = "search_type_scope_unsupported";

    private static final Set<SearchOperator> STRING_OPS = EnumSet.of(
            SearchOperator.EQ, SearchOperator.NEQ,
            SearchOperator.LIKE,
            SearchOperator.IN, SearchOperator.NOT_IN,
            SearchOperator.IS_NULL, SearchOperator.IS_NOT_NULL
    );
    private static final Set<SearchOperator> ORDERABLE_OPS = EnumSet.of(
            SearchOperator.EQ, SearchOperator.NEQ,
            SearchOperator.GT, SearchOperator.GTE, SearchOperator.LT, SearchOperator.LTE,
            SearchOperator.IN, SearchOperator.NOT_IN,
            SearchOperator.BETWEEN,
            SearchOperator.IS_NULL, SearchOperator.IS_NOT_NULL
    );
    private static final Set<SearchOperator> EQUALITY_OPS = EnumSet.of(
            SearchOperator.EQ, SearchOperator.NEQ,
            SearchOperator.IN, SearchOperator.NOT_IN,
            SearchOperator.IS_NULL, SearchOperator.IS_NOT_NULL
    );

    private SearchOperatorSupport() {
    }

    public static void validate(ResourceIdentifier field, PropertyType<?> type, SearchOperator op, @Nullable JsonNode value) {
        Set<SearchOperator> allowed = allowedOps(type);
        if (!allowed.contains(op)) {
            throw ApiException.validationFailed(field.toString(), OPERATOR_UNSUPPORTED, op.name());
        }
        validateValueShape(field, op, value);
    }

    public static void validateValueShape(ResourceIdentifier field, SearchOperator op, @Nullable JsonNode value) {
        switch (op) {
            case IS_NULL, IS_NOT_NULL -> {
                if (value != null && !value.isNull()) {
                    throw ApiException.validationFailed(field.toString(), OPERATOR_UNSUPPORTED, op.name());
                }
            }
            case IN, NOT_IN -> {
                if (value == null || !value.isArray() || value.isEmpty()) {
                    throw ApiException.validationFailed(field.toString(), OPERATOR_REQUIRES_ARRAY, op.name());
                }
            }
            case BETWEEN -> {
                if (value == null || !value.isArray() || value.size() != 2) {
                    throw ApiException.validationFailed(field.toString(), OPERATOR_REQUIRES_TWO_VALUES, op.name());
                }
            }
            default -> {
                if (value == null || value.isNull()) {
                    throw ApiException.validationFailed(field.toString(), OPERATOR_REQUIRES_VALUE, op.name());
                }
            }
        }
    }

    public static Set<SearchOperator> allowedOps(PropertyType<?> type) {
        return switch (type) {
            case PropertyType<?> t when
                    t == PropertyType.STRING ||
                            t == PropertyType.UUID ||
                            t == PropertyType.LIST_ITEM ||
                            t == PropertyType.STRING_LIST -> STRING_OPS;
            case PropertyType<?> t when
                    t == PropertyType.NUMBER ||
                            t == PropertyType.DECIMAL ||
                            t == PropertyType.DATE ||
                            t == PropertyType.INT_LIST -> ORDERABLE_OPS;
            default -> EQUALITY_OPS;
        };
    }

    public static String summarize(@Nullable String q, @Nullable List<SearchClause> filters) {
        StringBuilder sb = new StringBuilder();
        if (q != null && !q.isBlank()) {
            sb.append("q=").append(q.trim());
        }
        if (filters != null && !filters.isEmpty()) {
            if (!sb.isEmpty()) {
                sb.append(';');
            }
            Map<String, String> parts = new LinkedHashMap<>();
            for (SearchClause clause : filters) {
                parts.put(clause.field().toString(), clause.op().name());
            }
            sb.append("filters=").append(parts);
        }
        return sb.isEmpty() ? "" : sb.toString();
    }
}
