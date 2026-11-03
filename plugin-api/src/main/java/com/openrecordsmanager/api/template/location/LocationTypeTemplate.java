package com.openrecordsmanager.api.template.location;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.property.ManagedObjectKind;
import com.openrecordsmanager.api.template.PropertyAssignment;
import com.openrecordsmanager.api.template.TemplateComponent;
import com.openrecordsmanager.api.template.property.ObjectPropertyTemplate;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JsonDeserialize
public record LocationTypeTemplate(
        @Nullable ManagedObjectKind kind,
        @NotNull boolean abstractType,
        @Nullable ComponentReference<LocationTypeTemplate> baseType,
        @JsonDeserialize(using = PropertyAssignment.ListDeserializer.class) List<PropertyAssignment<?>> properties
) implements TemplateComponent {

    public LocationTypeTemplate {
        Objects.requireNonNull(properties, "Property 'properties' must not be null");
        validateKind(kind, abstractType);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Set<ComponentReference<? extends TemplateComponent>> getDependencies() {
        Stream<ComponentReference<? extends TemplateComponent>> propertyDeps = this.properties.stream()
                .map(PropertyAssignment::property);
        ComponentReference<LocationTypeTemplate> baseType = this.baseType;
        if (baseType == null) {
            return propertyDeps.collect(Collectors.toSet());
        }
        return Stream.concat(propertyDeps, Stream.of(baseType)).collect(Collectors.toSet());
    }

    public static void validateKind(@Nullable ManagedObjectKind kind, boolean abstractType) {
        if (kind == null) {
            if (!abstractType) {
                throw new IllegalArgumentException("Location type kind is required when not abstract");
            }
            return;
        }
        if (!kind.isLocation()) {
            throw new IllegalArgumentException("Location type kind must be USER or GROUP");
        }
    }

    public static final class Builder {
        private @Nullable ManagedObjectKind kind;
        private boolean abstractType = false;
        private @Nullable ComponentReference<LocationTypeTemplate> baseType;
        private final List<PropertyAssignment<?>> properties = new ArrayList<>();

        private Builder() {
        }

        public Builder kind(@Nullable ManagedObjectKind kind) {
            this.kind = kind;
            return this;
        }

        public Builder abstractType(boolean abstractType) {
            this.abstractType = abstractType;
            return this;
        }

        public Builder baseType(@Nullable ComponentReference<LocationTypeTemplate> baseType) {
            this.baseType = baseType;
            return this;
        }

        public Builder property(ComponentReference<? extends ObjectPropertyTemplate<?>> property) {
            this.properties.add(PropertyAssignment.ofUnknown(property));
            return this;
        }

        public <T> Builder property(ComponentReference<ObjectPropertyTemplate<T>> property, T defaultValue) {
            this.properties.add(PropertyAssignment.of(property, defaultValue));
            return this;
        }

        public LocationTypeTemplate build() {
            return new LocationTypeTemplate(
                    this.kind,
                    this.abstractType,
                    this.baseType,
                    List.copyOf(this.properties)
            );
        }
    }
}
