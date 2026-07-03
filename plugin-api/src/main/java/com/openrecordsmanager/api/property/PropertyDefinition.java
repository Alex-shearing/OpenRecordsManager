package com.openrecordsmanager.api.property;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.expression.ExpressionBuilder;
import com.openrecordsmanager.api.list.ListDefinition;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@JsonDeserialize(builder = PropertyDefinition.Builder.class)
public class PropertyDefinition<T> implements Component {
    private final PropertyType<T> type;
    private final String name;
    private final String description;
    @Nullable
    private final ListDefinition listType;
    @Nullable
    private final ExpressionBuilder validator;
    @Nullable
    private final T defaultValue;
    @Nullable
    private final ExpressionBuilder securityFilter;

    private final Set<Component> dependencies = new HashSet<>();

    private PropertyDefinition(
            PropertyType<T> type,
            String name,
            String description,
            @Nullable ListDefinition listType,
            @Nullable ExpressionBuilder validator,
            @Nullable T defaultValue,
            @Nullable ExpressionBuilder securityFilter,
            Set<Component> additionalDependencies
    ) {
        Objects.requireNonNull(type, "Property 'type' must not be null");
        Objects.requireNonNull(name, "Property 'name' must not be null");
        Objects.requireNonNull(description, "Property 'description' must not be null");

        this.type = type;
        this.name = name;
        this.description = description;
        this.listType = listType;
        this.validator = validator;
        this.defaultValue = defaultValue;
        this.securityFilter = securityFilter;

        if (listType != null) this.dependencies.add(listType);
        if (validator != null) this.dependencies.addAll(List.of(validator.dependencies()));
        if (securityFilter != null) this.dependencies.addAll(List.of(securityFilter.dependencies()));
        this.dependencies.addAll(additionalDependencies);
    }

    public PropertyType<T> getType() {
        return this.type;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public @Nullable ListDefinition getListType() {
        return listType;
    }

    public @Nullable ExpressionBuilder getValidator() {
        return this.validator;
    }

    public @Nullable T getDefaultValue() {
        return defaultValue;
    }

    public @Nullable ExpressionBuilder getSecurityFilter() {
        return securityFilter;
    }

    public Set<Component> getDependencies() {
        return dependencies;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PropertyDefinition<?> that)) return false;
        return Objects.equals(type, that.type) &&
                Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) &&
                Objects.equals(listType, that.listType) &&
                Objects.equals(validator, that.validator) &&
                Objects.equals(defaultValue, that.defaultValue) &&
                Objects.equals(securityFilter, that.securityFilter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name, description, listType, validator, defaultValue, securityFilter);
    }

    public static <K> Builder<K> builder(String name, PropertyType<K> type) {
        return new Builder<>(name, type);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder<T> {
        private PropertyType<T> type;
        private String name;
        private String description = "";
        private ListDefinition listType = null;
        private ExpressionBuilder validator = null;
        private T defaultValue = null;
        private ExpressionBuilder securityFilter = null;

        private final Set<Component> dependencies = new HashSet<>();

        private Builder(String name, PropertyType<T> type) {
            this.name = name;
            this.type = type;
        }
        
        @JsonCreator
        private Builder() {
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

        public Builder<T> listType(ListDefinition listType) {
            if (!this.type.allowsList()) {
                throw new IllegalArgumentException("listType can only be used for list or list item");
            }
            this.listType = listType;
            return this;
        }

        public Builder<T> validator(String validator, PropertyDefinition<?>... definition) {
            return this.validator(ExpressionBuilder.from(validator, definition));
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
            return this.securityFilter(ExpressionBuilder.from(filter, definition));
        }

        public Builder<T> dependency(Component... component) {
            this.dependencies.addAll(List.of(component));
            return this;
        }

        public PropertyDefinition<T> build() {
            return new PropertyDefinition<>(
                    this.type,
                    this.name,
                    this.description,
                    this.listType,
                    this.validator,
                    this.defaultValue,
                    this.securityFilter,
                    this.dependencies
            );
        }
    }
}
