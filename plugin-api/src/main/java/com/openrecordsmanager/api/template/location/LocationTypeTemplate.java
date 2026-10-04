package com.openrecordsmanager.api.template.location;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.location.LocationKind;
import com.openrecordsmanager.api.template.PropertyAssignment;
import com.openrecordsmanager.api.template.TemplateComponent;
import com.openrecordsmanager.api.template.property.ObjectPropertyTemplate;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.*;
import java.util.stream.Collectors;

@JsonDeserialize
public record LocationTypeTemplate(
        LocationKind kind,
        @JsonDeserialize(using = PropertyAssignment.ListDeserializer.class) List<PropertyAssignment<?>> properties
) implements TemplateComponent {

    public LocationTypeTemplate {
        Objects.requireNonNull(kind, "Property 'kind' must not be null");
        Objects.requireNonNull(properties, "Property 'properties' must not be null");
        if (kind == LocationKind.ANY) {
            throw new IllegalArgumentException("Location type kind must be USER or GROUP");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Set<ComponentReference<? extends TemplateComponent>> getDependencies() {
        return this.properties.stream()
                .map(PropertyAssignment::property)
                .collect(Collectors.toSet());
    }

    public static final class Builder {
        private @Nullable LocationKind kind;
        private final List<PropertyAssignment<?>> properties = new ArrayList<>();

        private Builder() {
        }

        public Builder kind(LocationKind kind) {
            this.kind = kind;
            return this;
        }

        public Builder property(ComponentReference<? extends ObjectPropertyTemplate<?>> property) {
            this.properties.add(PropertyAssignment.ofUnknown(property));
            return this;
        }

        public Builder property(ObjectPropertyTemplate<?> property) {
            return this.property(ComponentReference.of(property));
        }

        public <T> Builder property(ComponentReference<ObjectPropertyTemplate<T>> property, T defaultValue) {
            this.properties.add(PropertyAssignment.of(property, defaultValue));
            return this;
        }

        public <T> Builder property(ObjectPropertyTemplate<T> property, T defaultValue) {
            return this.property(ComponentReference.of(property), defaultValue);
        }

        public LocationTypeTemplate build() {
            return new LocationTypeTemplate(
                    Objects.requireNonNull(this.kind, "Property 'kind' must not be null"),
                    List.copyOf(this.properties)
            );
        }
    }
}
