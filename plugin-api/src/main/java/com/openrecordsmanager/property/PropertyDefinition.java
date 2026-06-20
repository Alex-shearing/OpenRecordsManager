package com.openrecordsmanager.property;

import com.openrecordsmanager.RecordObject;
import com.openrecordsmanager.RegisterableComponent;
import com.openrecordsmanager.list.ListDefinition;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.BiPredicate;

public class PropertyDefinition<T> implements RegisterableComponent {
    private final String id;
    private final PropertyType<T> type;
    private final String name;
    private final String description;
    @Nullable
    private final ListDefinition listType;
    @Nullable
    private final BiPredicate<RecordObject, PropertyDefinition<T>> validator;
    @Nullable
    private final T defaultValue;
    @Nullable
    private final String securityFilter;

    private PropertyDefinition(String id, PropertyType<T> type, String name, String description, @Nullable ListDefinition listType, @Nullable BiPredicate<RecordObject, PropertyDefinition<T>> validator, @Nullable T defaultValue, @Nullable String securityFilter) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.description = description;
        this.listType = listType;
        this.validator = validator;
        this.defaultValue = defaultValue;
        this.securityFilter = securityFilter;
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

    public @Nullable BiPredicate<RecordObject, PropertyDefinition<T>> getValidator() {
        return validator;
    }

    public @Nullable T getDefaultValue() {
        return defaultValue;
    }

    public @Nullable String getSecurityFilter() {
        return securityFilter;
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
        private BiPredicate<RecordObject, PropertyDefinition<T>> validator;
        private T defaultValue;
        @Nullable
        private String securityFilter;

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

        public Builder<T> validator(BiPredicate<RecordObject, PropertyDefinition<T>> validator) {
            this.validator = validator;
            return this;
        }

        public Builder<T> defaultValue(T defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder<T> securityFilter(String filter) {
            this.securityFilter = filter;
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
