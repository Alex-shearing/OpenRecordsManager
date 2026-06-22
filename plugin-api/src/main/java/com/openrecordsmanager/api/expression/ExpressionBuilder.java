package com.openrecordsmanager.api.expression;

import com.openrecordsmanager.api.property.PropertyDefinition;

public record ExpressionBuilder(String filter, PropertyDefinition<?>[] dependencies) {
    public static ExpressionBuilder from(String filter, PropertyDefinition<?>... components) {
        return new ExpressionBuilder(filter, components);
    }
}
