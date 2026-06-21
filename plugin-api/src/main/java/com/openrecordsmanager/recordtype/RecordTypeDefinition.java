package com.openrecordsmanager.recordtype;

import com.openrecordsmanager.RegisterableComponent;
import com.openrecordsmanager.property.PropertyDefinition;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record RecordTypeDefinition(String id, String name, String description, Set<PropertyDefinition<?>> properties,
                                   @Nullable Set<String> allowedContentTypes, @Nullable String securityFilter,
                                   SecurityFilterUsage securityFilterUsage) implements RegisterableComponent {

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static class Builder {
        private final String id;
        private String name;
        private String description = "";
        private final Set<PropertyDefinition<?>> properties = new HashSet<>();
        private Set<String> allowedContentTypes = null;
        private String securityFilter = null;
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
            this.properties.add(property);
            return this;
        }

        public Builder securityFilterUsage(SecurityFilterUsage filterUsage) {
            this.securityFilterUsage = filterUsage;
            return this;
        }

        public Builder securityFilter(String filter, SecurityFilterUsage filterUsage) {
            this.securityFilter = filter;
            this.securityFilterUsage = filterUsage;
            return this;
        }

        /**
         * This will be determined using the {@link java.nio.file.Files#probeContentType(Path)} method.
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
