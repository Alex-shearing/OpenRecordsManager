package com.openrecordsmanager.api.template.recordtype;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.template.expression.ExpressionBuilder;
import com.openrecordsmanager.api.template.property.PropertyDefinition;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JsonDeserialize
@NullMarked
public record RecordTypeDefinition(
        String name,
        String description,
        Map<ComponentReference<PropertyDefinition<?>>, ?> properties,
        @Nullable Set<String> allowedContentTypes,
        @Nullable ExpressionBuilder securityFilter,
        @JsonDeserialize(using = SecurityFilterUsage.Deserializer.class) SecurityFilterUsage securityFilterUsage
) implements Component {

    public RecordTypeDefinition {
        Objects.requireNonNull(name, "Property 'name' must not be null");
        Objects.requireNonNull(description, "Property 'description' must not be null");
        Objects.requireNonNull(properties, "Property 'properties' must not be null");
        Objects.requireNonNull(securityFilterUsage, "Property 'securityFilterUsage' must not be null");
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    @Override
    public Set<ComponentReference<? extends Component>> getDependencies() {
        if (this.securityFilter == null) {
            return Set.copyOf(this.properties.keySet());
        }

        return Stream.concat(this.properties.keySet().stream(), this.securityFilter.dependencies().stream())
                .collect(Collectors.toSet());
    }

    public static class Builder {
        private String name;
        private String description = "";
        private final Map<ComponentReference<PropertyDefinition<?>>, @Nullable Object> properties = new HashMap<>();
        @Nullable
        private Set<String> allowedContentTypes = null;
        @Nullable
        private ExpressionBuilder securityFilter = null;
        private SecurityFilterUsage securityFilterUsage = SecurityFilterUsage.HIDE_FILES;

        private Builder(String id) {
            this.name = id;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder properties(Set<ComponentReference<PropertyDefinition<?>>> properties) {
            for (ComponentReference<PropertyDefinition<?>> property : properties) {
                this.properties.put(property, null);
            }
            return this;
        }

        public Builder property(ComponentReference<PropertyDefinition<?>> property) {
            this.properties.put(property, null);
            return this;
        }

        public Builder property(PropertyDefinition<?> property) {
            return this.property(ComponentReference.of(property));
        }

        public <T> Builder property(ComponentReference<PropertyDefinition<T>> property, T defaultValue) {
            this.properties.put(property.widen(def -> def), defaultValue);
            return this;
        }

        public <T> Builder property(PropertyDefinition<T> property, T defaultValue) {
            return this.property(ComponentReference.of(property), defaultValue);
        }

        public Builder securityFilter(SecurityFilterUsage filterUsage, String filter, PropertyDefinition<?>... properties) {
            List<ComponentReference<PropertyDefinition<?>>> deps = new ArrayList<>(properties.length);
            for (PropertyDefinition<?> propertyDefinition : properties) {
                deps.add(ComponentReference.of(propertyDefinition));
            }

            return this.securityFilter(filterUsage, new ExpressionBuilder(filter, deps));
        }

        public Builder securityFilter(SecurityFilterUsage filterUsage, ExpressionBuilder filter) {
            this.securityFilter = filter;
            this.securityFilterUsage = filterUsage;
            return this;
        }

        /**
         * This will be determined using the {@link Files#probeContentType(Path)} method.
         * Supports using asterisk (*) wildcard characters (i.e. `*`, `text/*`).
         */
        public Builder allowedContentTypes(String... allowedContentTypes) {
            if (this.allowedContentTypes == null) {
                this.allowedContentTypes = new HashSet<>();
            }
            this.allowedContentTypes.addAll(List.of(allowedContentTypes));
            return this;
        }

        public RecordTypeDefinition build() {
            return new RecordTypeDefinition(
                    this.name,
                    this.description,
                    this.properties,
                    this.allowedContentTypes,
                    this.securityFilter,
                    this.securityFilterUsage
            );
        }
    }
}
