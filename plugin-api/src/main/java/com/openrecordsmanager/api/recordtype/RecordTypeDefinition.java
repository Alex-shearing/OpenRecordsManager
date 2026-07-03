package com.openrecordsmanager.api.recordtype;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.expression.ExpressionBuilder;
import com.openrecordsmanager.api.property.PropertyDefinition;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@JsonDeserialize(builder = RecordTypeDefinition.Builder.class)
public final class RecordTypeDefinition implements Component {
    private final String name;
    private final String description;
    private final Map<ComponentReference<PropertyDefinition<?>>, ?> properties;
    private final @Nullable Set<String> allowedContentTypes;
    private final @Nullable ExpressionBuilder securityFilter;
    private final SecurityFilterUsage securityFilterUsage;

    private final Set<ComponentReference<? extends Component>> dependencies = new HashSet<>();

    public RecordTypeDefinition(
            String name,
            String description,
            Map<ComponentReference<PropertyDefinition<?>>, ?> properties,
            @Nullable Set<String> allowedContentTypes,
            @Nullable ExpressionBuilder securityFilter,
            SecurityFilterUsage securityFilterUsage
    ) {
        Objects.requireNonNull(name, "Property 'name' must not be null");
        Objects.requireNonNull(description, "Property 'description' must not be null");
        Objects.requireNonNull(properties, "Property 'properties' must not be null");
        Objects.requireNonNull(securityFilterUsage, "Property 'securityFilterUsage' must not be null");

        this.name = name;
        this.description = description;
        this.properties = properties;
        this.allowedContentTypes = allowedContentTypes;
        this.securityFilter = securityFilter;
        this.securityFilterUsage = securityFilterUsage;

        this.dependencies.addAll(this.properties.keySet());

        if (securityFilter != null) this.dependencies.addAll(securityFilter.dependencies());
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public Map<ComponentReference<PropertyDefinition<?>>, ?> properties() {
        return properties;
    }

    public @Nullable Set<String> allowedContentTypes() {
        return allowedContentTypes;
    }

    public @Nullable ExpressionBuilder securityFilter() {
        return securityFilter;
    }

    public SecurityFilterUsage securityFilterUsage() {
        return securityFilterUsage;
    }

    @Override
    public Set<ComponentReference<? extends Component>> getDependencies() {
        return dependencies;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RecordTypeDefinition that)) return false;
        return Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) &&
                Objects.equals(properties, that.properties) &&
                Objects.equals(allowedContentTypes, that.allowedContentTypes) &&
                Objects.equals(securityFilter, that.securityFilter) &&
                securityFilterUsage == that.securityFilterUsage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, properties, allowedContentTypes, securityFilter, securityFilterUsage);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private String name;
        private String description = "";
        private final Map<ComponentReference<PropertyDefinition<?>>, Object> properties = new HashMap<>();
        private Set<String> allowedContentTypes = null;
        private ExpressionBuilder securityFilter = null;
        private SecurityFilterUsage securityFilterUsage = SecurityFilterUsage.HIDE_FILES;

        private Builder(String id) {
            this.name = id;
        }

        @JsonCreator
        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        @JsonSetter("properties")
        public Builder properties(Set<ComponentReference<PropertyDefinition<?>>> properties) {
            for (ComponentReference<PropertyDefinition<?>> property : properties) {
                this.properties.put(property, null);
            }
            return this;
        }

        @JsonIgnore
        public Builder property(ComponentReference<PropertyDefinition<?>> property) {
            this.properties.put(property, null);
            return this;
        }

        @JsonIgnore
        public Builder property(PropertyDefinition<?> property) {
            return this.property(ComponentReference.of(property));
        }

        @JsonIgnore
        public <T> Builder property(ComponentReference<PropertyDefinition<T>> property, T defaultValue) {
            this.properties.put(property.map(def -> def), defaultValue);
            return this;
        }

        @JsonIgnore
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
