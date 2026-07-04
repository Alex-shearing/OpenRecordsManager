package com.openrecordsmanager.api.template.property;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.template.ExpressionBuilder;
import com.openrecordsmanager.api.template.list.ListDefinition;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.*;

@JsonDeserialize
public record PropertyDefinition<T>(
        PropertyType<T> type,
        String name,
        String description,
        @Nullable ComponentReference<ListDefinition> listType,
        @Nullable ExpressionBuilder validator,
        @Nullable T defaultValue,
        @Nullable ExpressionBuilder securityFilter
) implements Component {

    public PropertyDefinition {
        Objects.requireNonNull(type, "Property 'type' must not be null");
        Objects.requireNonNull(name, "Property 'name' must not be null");
        Objects.requireNonNull(description, "Property 'description' must not be null");
    }

    public Set<ComponentReference<? extends Component>> getDependencies() {
        Set<ComponentReference<? extends Component>> dependencies = new HashSet<>();

        if (this.listType != null) dependencies.add(this.listType);
        if (this.validator != null) dependencies.addAll(this.validator.dependencies());
        if (this.securityFilter != null) dependencies.addAll(this.securityFilter.dependencies());
        return dependencies;
    }

    public static <K> Builder<K> builder(String name, PropertyType<K> type) {
        return new Builder<>(name, type);
    }

    public static class Builder<T> {
        private PropertyType<T> type;
        private String name;
        private String description = "";
        @Nullable
        private ComponentReference<ListDefinition> listType = null;
        @Nullable
        private ExpressionBuilder validator = null;
        @Nullable
        private T defaultValue = null;
        @Nullable
        private ExpressionBuilder securityFilter = null;

        private Builder(String name, PropertyType<T> type) {
            this.name = name;
            this.type = type;
        }

        public Builder<T> type(PropertyType<T> type) {
            this.type = type;
            return this;
        }

        public Builder<T> name(String name) {
            this.name = name;
            return this;
        }

        public Builder<T> description(String description) {
            this.description = description;
            return this;
        }

        public Builder<T> listType(ComponentReference<ListDefinition> listType) {
            if (!this.type.allowsList()) {
                throw new IllegalArgumentException("listType can only be used for list or list item");
            }
            this.listType = listType;
            return this;
        }

        public Builder<T> listType(ListDefinition listType) {
            return this.listType(ComponentReference.of(listType));
        }

        public Builder<T> validator(String validator, PropertyDefinition<?>... definition) {
            List<ComponentReference<PropertyDefinition<?>>> deps = Arrays.stream(definition)
                    .map(ComponentReference::<PropertyDefinition<?>>of)
                    .toList();

            return this.validator(new ExpressionBuilder(validator, deps));
        }

        public Builder<T> validator(ExpressionBuilder expression) {
            this.validator = expression;
            return this;
        }

        public Builder<T> defaultValue(T defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder<T> securityFilter(ExpressionBuilder expression) {
            this.securityFilter = expression;
            return this;
        }

        public Builder<T> securityFilter(String filter, PropertyDefinition<?>... definition) {
            List<ComponentReference<PropertyDefinition<?>>> deps = new ArrayList<>(definition.length);
            for (PropertyDefinition<?> propertyDefinition : definition) {
                deps.add(ComponentReference.of(propertyDefinition));
            }

            return this.securityFilter(new ExpressionBuilder(filter, deps));
        }

        public PropertyDefinition<T> build() {
            T typedDefaultValue = this.defaultValue;

            return new PropertyDefinition<>(
                    this.type,
                    this.name,
                    this.description,
                    this.listType,
                    this.validator,
                    typedDefaultValue,
                    this.securityFilter
            );
        }
    }
}
