package com.openrecordsmanager.api.template.expression;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.template.property.PropertyDefinition;

import java.util.List;

public record ExpressionBuilder(String filter, List<ComponentReference<PropertyDefinition<?>>> dependencies) {

    @JsonCreator
    public static ExpressionBuilder from(String filter) {
        return new ExpressionBuilder(filter, List.of());
    }
}
