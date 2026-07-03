package com.openrecordsmanager.api.recordtype;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.openrecordsmanager.api.Component;
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
    private final Map<PropertyDefinition<?>, ?> properties;
    private final @Nullable Set<String> allowedContentTypes;
    private final @Nullable ExpressionBuilder securityFilter;
    private final SecurityFilterUsage securityFilterUsage;

    private final Set<Component> dependencies = new HashSet<>();

    public RecordTypeDefinition(
            String name,
            String description,
            Map<PropertyDefinition<?>, ?> properties,
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

        if (securityFilter != null) this.dependencies.addAll(List.of(securityFilter.dependencies()));
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

    public Map<PropertyDefinition<?>, ?> properties() {
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
    public Set<Component> getDependencies() {
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
        private final Map<PropertyDefinition<?>, Object> properties = new HashMap<>();
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

        public Builder property(PropertyDefinition<?> property) {
            this.properties.put(property, null);
            return this;
        }

        public <T> Builder property(PropertyDefinition<T> property, T defaultValue) {
            this.properties.put(property, defaultValue);
            return this;
        }

        public Builder securityFilter(SecurityFilterUsage filterUsage, String filter, PropertyDefinition<?>... components) {
            return this.securityFilter(filterUsage, ExpressionBuilder.from(filter, components));
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
