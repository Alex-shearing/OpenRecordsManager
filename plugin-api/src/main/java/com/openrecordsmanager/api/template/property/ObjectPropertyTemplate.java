package com.openrecordsmanager.api.template.property;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.template.ExpressionBuilder;
import com.openrecordsmanager.api.template.TemplateComponent;
import com.openrecordsmanager.api.template.list.ListTemplate;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.*;

@JsonDeserialize
public record ObjectPropertyTemplate<T>(
        PropertyType<T> type,
        @Nullable ComponentReference<ListTemplate> listType,
        @Nullable ExpressionBuilder validator,
        @Nullable T defaultValue,
        @Nullable ExpressionBuilder securityFilter,
        boolean userHidden
) implements TemplateComponent {

    public ObjectPropertyTemplate {
        Objects.requireNonNull(type, "Property 'type' must not be null");
    }

    public Set<ComponentReference<? extends TemplateComponent>> getDependencies() {
        Set<ComponentReference<? extends TemplateComponent>> dependencies = new HashSet<>();

        if (this.listType != null) dependencies.add(this.listType);
        if (this.validator != null) dependencies.addAll(this.validator.dependencies());
        if (this.securityFilter != null) dependencies.addAll(this.securityFilter.dependencies());
        return dependencies;
    }

    public static <K> Builder<K> builder(PropertyType<K> type) {
        return new Builder<>(type);
    }

    public static class Builder<T> {
        private PropertyType<T> type;
        @Nullable
        private ComponentReference<ListTemplate> listType = null;
        @Nullable
        private ExpressionBuilder validator = null;
        @Nullable
        private T defaultValue = null;
        @Nullable
        private ExpressionBuilder securityFilter = null;
        private boolean userHidden = false;

        private Builder(PropertyType<T> type) {
            this.type = type;
        }

        /**
         * Set the property type
         *
         * @param type the type for this property
         * @return this builder
         */
        public Builder<T> type(PropertyType<T> type) {
            this.type = type;
            return this;
        }

        /**
         * The type of list for a {@link PropertyType#LIST_ITEM} or {@link PropertyType#LIST_MULTIPLE} type.
         *
         * @param listType reference to the type of list to use
         * @return this builder
         */
        public Builder<T> listType(ComponentReference<ListTemplate> listType) {
            if (!this.type.allowsList()) {
                throw new IllegalArgumentException("listType can only be used for list or list item");
            }
            this.listType = listType;
            return this;
        }

        /**
         * The type of list for a {@link PropertyType#LIST_ITEM} or {@link PropertyType#LIST_MULTIPLE} type.
         *
         * @param listType the type of list to use
         * @return this builder
         */
        public Builder<T> listType(ListTemplate listType) {
            return this.listType(ComponentReference.of(listType));
        }

        /**
         * Apply a validation expression to the property
         *
         * @param validator    the validation CET string
         * @param dependencies dependencies used in the filter pattern
         * @return this builder
         */
        public Builder<T> validator(String validator, ObjectPropertyTemplate<?>... dependencies) {
            List<ComponentReference<TemplateComponent>> deps = Arrays.stream(dependencies)
                    .<ComponentReference<TemplateComponent>>map(ComponentReference::of)
                    .toList();

            return this.validator(new ExpressionBuilder(validator, deps));
        }

        /**
         * Apply a validation expression to the property
         *
         * @param expression the expression to check
         * @return this builder
         */
        public Builder<T> validator(ExpressionBuilder expression) {
            this.validator = expression;
            return this;
        }

        /**
         * Set a default value for the property.
         *
         * @param defaultValue the default value
         * @return this builder
         */
        public Builder<T> defaultValue(T defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        /**
         * Apply a security filter to the object this property exists on.
         *
         * @param expression the filter expression to check
         * @return this builder
         */
        public Builder<T> securityFilter(ExpressionBuilder expression) {
            this.securityFilter = expression;
            return this;
        }

        /**
         * Apply a security filter to the object this property exists on.
         *
         * @param filter       the filter CET string
         * @param dependencies dependencies used in the filter pattern
         * @return this builder
         */
        public Builder<T> securityFilter(String filter, ObjectPropertyTemplate<?>... dependencies) {
            List<ComponentReference<TemplateComponent>> deps = Arrays.stream(dependencies)
                    .<ComponentReference<TemplateComponent>>map(ComponentReference::of)
                    .toList();

            return this.securityFilter(new ExpressionBuilder(filter, deps));
        }

        /**
         * Hides this property from users (never returned by API responses).
         *
         * @return this builder
         */
        public Builder<T> userHidden() {
            this.userHidden = true;
            return this;
        }

        public ObjectPropertyTemplate<T> build() {
            return new ObjectPropertyTemplate<>(
                    this.type,
                    this.listType,
                    this.validator,
                    this.defaultValue,
                    this.securityFilter,
                    this.userHidden
            );
        }
    }
}
