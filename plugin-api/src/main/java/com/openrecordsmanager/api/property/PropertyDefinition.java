package com.openrecordsmanager.api.property;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.expression.ExpressionBuilder;
import com.openrecordsmanager.api.list.ListDefinition;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@SuppressWarnings("unused")
public class PropertyDefinition<T> implements Component {
    private final String id;
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

    private PropertyDefinition(String id, PropertyType<T> type, String name, String description, @Nullable ListDefinition listType, @Nullable ExpressionBuilder validator, @Nullable T defaultValue, @Nullable ExpressionBuilder securityFilter) {
        this.id = id;
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
    public String id() {
        return this.id;
    }

    public static <K> Builder<K> builder(String id, PropertyType<K> type) {
        return new Builder<>(id, type);
    }

    public static class Builder<T> {
        private final String id;
        private final PropertyType<T> type;
        private String name;
        private String description;
        @Nullable
        private ListDefinition listType;
        @Nullable
        private ExpressionBuilder validator;
        private T defaultValue;
        @Nullable
        private ExpressionBuilder securityFilter;

        private Builder(String id, PropertyType<T> type) {
            this.id = id;
            this.name = id;
            this.description = "";
            this.type = type;
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
            if (this.type != PropertyType.LIST_MULTIPLE && this.type != PropertyType.LIST_ITEM) {
                throw new IllegalArgumentException("listType can only be used for list or list item");
            }
            this.listType = listType;
            return this;
        }

        public Builder<T> validator(String validator, PropertyDefinition<?>... definition) {
            this.validator = ExpressionBuilder.from(validator, definition);
            return this;
        }

        public Builder<T> defaultValue(T defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder<T> securityFilter(String filter, PropertyDefinition<?>... definition) {
            this.securityFilter = ExpressionBuilder.from(filter, definition);
            return this;
        }

        public PropertyDefinition<T> build() {
            Objects.requireNonNull(this.id, "Property 'id' must not be null");
            Objects.requireNonNull(this.type, "Property 'type' must not be null");
            Objects.requireNonNull(this.name, "Property 'name' must not be null");
            Objects.requireNonNull(this.description, "Property 'description' must not be null");
            return new PropertyDefinition<>(this.id, this.type, this.name, this.description, this.listType, this.validator, this.defaultValue, this.securityFilter);
        }
    }
}
