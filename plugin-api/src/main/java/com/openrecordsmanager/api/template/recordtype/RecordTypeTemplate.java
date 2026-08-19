package com.openrecordsmanager.api.template.recordtype;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.template.ExpressionBuilder;
import com.openrecordsmanager.api.template.TemplateComponent;
import com.openrecordsmanager.api.template.property.ObjectPropertyTemplate;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JsonDeserialize
public record RecordTypeTemplate(
        String name,
        String description,
        Map<ComponentReference<ObjectPropertyTemplate<?>>, ?> properties,
        @Nullable Set<String> allowedContentTypes,
        @Nullable ExpressionBuilder securityFilter,
        @JsonDeserialize(using = SecurityFilterUsage.Deserializer.class) SecurityFilterUsage securityFilterUsage
) implements TemplateComponent {

    public RecordTypeTemplate {
        Objects.requireNonNull(name, "Property 'name' must not be null");
        Objects.requireNonNull(description, "Property 'description' must not be null");
        Objects.requireNonNull(properties, "Property 'properties' must not be null");
        Objects.requireNonNull(securityFilterUsage, "Property 'securityFilterUsage' must not be null");
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    @Override
    public Set<ComponentReference<? extends TemplateComponent>> getDependencies() {
        if (this.securityFilter == null) {
            return Set.copyOf(this.properties.keySet());
        }

        return Stream.concat(this.properties.keySet().stream(), this.securityFilter.dependencies().stream())
                .collect(Collectors.toSet());
    }

    public static class Builder {
        private String name;
        private String description = "";
        private final Map<ComponentReference<ObjectPropertyTemplate<?>>, @Nullable Object> properties = new HashMap<>();
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

        public Builder properties(Set<ComponentReference<ObjectPropertyTemplate<?>>> properties) {
            for (ComponentReference<ObjectPropertyTemplate<?>> property : properties) {
                this.properties.put(property, null);
            }
            return this;
        }

        public Builder property(ComponentReference<ObjectPropertyTemplate<?>> property) {
            this.properties.put(property, null);
            return this;
        }

        public Builder property(ObjectPropertyTemplate<?> property) {
            return this.property(ComponentReference.of(property));
        }

        public <T> Builder property(ComponentReference<ObjectPropertyTemplate<T>> property, T defaultValue) {
            this.properties.put(property.widen(def -> def), defaultValue);
            return this;
        }

        public <T> Builder property(ObjectPropertyTemplate<T> property, T defaultValue) {
            return this.property(ComponentReference.of(property), defaultValue);
        }

        @SafeVarargs
        public final Builder securityFilter(SecurityFilterUsage filterUsage, String filter, ObjectPropertyTemplate<TemplateComponent>... dependencies) {
            List<ComponentReference<TemplateComponent>> deps = Arrays.stream(dependencies)
                    .<ComponentReference<TemplateComponent>>map(ComponentReference::of)
                    .toList();

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

        public RecordTypeTemplate build() {
            return new RecordTypeTemplate(
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
