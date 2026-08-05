package com.openrecordsmanager.api.template;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.template.property.ObjectPropertyTemplate;

import java.util.List;
import java.util.Objects;

public record ExpressionBuilder(String filter, List<ComponentReference<ObjectPropertyTemplate<?>>> dependencies) {

    public ExpressionBuilder {
        Objects.requireNonNull(filter, "Property 'filter' must not be null");
        Objects.requireNonNull(dependencies, "Property 'dependencies' must not be null");
    }

    @JsonCreator
    public static ExpressionBuilder from(String filter) {
        return new ExpressionBuilder(filter, List.of());
    }
}
