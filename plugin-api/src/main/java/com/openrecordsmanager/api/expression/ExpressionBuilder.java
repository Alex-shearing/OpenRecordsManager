package com.openrecordsmanager.api.expression;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.openrecordsmanager.api.property.PropertyDefinition;

import java.util.Objects;

public record ExpressionBuilder(String filter, PropertyDefinition<?>[] dependencies) {

    @JsonCreator
    public static ExpressionBuilder from(String filter) {
        return new ExpressionBuilder(filter, new PropertyDefinition[0]);
    }

    public static ExpressionBuilder from(String filter, PropertyDefinition<?>... components) {
        return new ExpressionBuilder(filter, components);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ExpressionBuilder(String filter1, PropertyDefinition<?>[] dependencies1))) return false;
        return Objects.equals(filter, filter1) && Objects.deepEquals(dependencies, dependencies1);
    }
}
