package com.openrecordsmanager.api.recordtype;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.expression.ExpressionBuilder;
import com.openrecordsmanager.api.property.PropertyDefinition;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class RecordTypeDefinition implements Component {
    private final String id;
    private final String name;
    private final String description;
    private final Map<PropertyDefinition<?>, ?> properties;
    private final @Nullable Set<String> allowedContentTypes;
    private final @Nullable ExpressionBuilder securityFilter;
    private final SecurityFilterUsage securityFilterUsage;

    private final Set<Component> dependencies = new HashSet<>();

    public RecordTypeDefinition(String id, String name, String description, Map<PropertyDefinition<?>, ?> properties,
                                @Nullable Set<String> allowedContentTypes, @Nullable ExpressionBuilder securityFilter,
                                SecurityFilterUsage securityFilterUsage) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.properties = properties;
        this.allowedContentTypes = allowedContentTypes;
        this.securityFilter = securityFilter;
        this.securityFilterUsage = securityFilterUsage;

        this.dependencies.addAll(this.properties.keySet());

        if (securityFilter != null) this.dependencies.addAll(List.of(securityFilter.dependencies()));
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    @Override
    public String id() {
        return id;
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

    public static class Builder {
        private final String id;
        private String name;
        private String description = "";
        private final Map<PropertyDefinition<?>, Object> properties = new HashMap<>();
        private Set<String> allowedContentTypes = null;
        private ExpressionBuilder securityFilter = null;
        private SecurityFilterUsage securityFilterUsage = SecurityFilterUsage.HIDE_FILES;

        private Builder(String id) {
            this.id = id;
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

        public Builder property(PropertyDefinition<?> property) {
            this.properties.put(property, null);
            return this;
        }

        public <T> Builder property(PropertyDefinition<T> property, T defaultValue) {
            this.properties.put(property, defaultValue);
            return this;
        }

        public Builder securityFilterUsage(SecurityFilterUsage filterUsage) {
            this.securityFilterUsage = filterUsage;
            return this;
        }

        public Builder securityFilter(String filter, SecurityFilterUsage filterUsage) {
            return this.securityFilter(ExpressionBuilder.from(filter), filterUsage);
        }

        public Builder securityFilter(ExpressionBuilder filter, SecurityFilterUsage filterUsage) {
            this.securityFilter = filter;
            this.securityFilterUsage = filterUsage;
            return this;
        }

        /**
         * This will be determined using the {@link Files#probeContentType(Path)} method.
         * Supports using asterisk (*) wildcard characters (i.e. `*`, `text/*`).
         */
        public Builder supportsFile(String... allowedContentTypes) {
            if (this.allowedContentTypes == null) {
                this.allowedContentTypes = new HashSet<>();
            }
            this.allowedContentTypes.addAll(List.of(allowedContentTypes));
            return this;
        }

        public RecordTypeDefinition build() {
            Objects.requireNonNull(this.id, "Property 'id' must not be null");
            Objects.requireNonNull(this.name, "Property 'name' must not be null");
            Objects.requireNonNull(this.description, "Property 'description' must not be null");
            Objects.requireNonNull(this.properties, "Property 'properties' must not be null");

            return new RecordTypeDefinition(
                    this.id,
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
