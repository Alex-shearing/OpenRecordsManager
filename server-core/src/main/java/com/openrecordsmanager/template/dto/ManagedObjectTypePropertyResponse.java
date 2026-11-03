package com.openrecordsmanager.template.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openrecordsmanager.property.dto.ObjectPropertyResponse;
import com.openrecordsmanager.template.ManagedObjectTypeProperty;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public record ManagedObjectTypePropertyResponse(
        @NotNull ObjectPropertyResponse property,
        @JsonProperty("default") @Nullable JsonNode defaultValue
) {
    public static ManagedObjectTypePropertyResponse of(ManagedObjectTypeProperty<?> property) {
        return new ManagedObjectTypePropertyResponse(
                ObjectPropertyResponse.of(property.getProperty()),
                property.getDefault()
        );
    }

    public static Set<ManagedObjectTypePropertyResponse> ofAll(
            Collection<ManagedObjectTypeProperty<?>> properties
    ) {
        return properties.stream()
                .filter(property -> !property.getProperty().isSystemProperty())
                .map(ManagedObjectTypePropertyResponse::of)
                .collect(Collectors.toSet());
    }
}
