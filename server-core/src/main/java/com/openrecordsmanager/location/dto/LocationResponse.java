package com.openrecordsmanager.location.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.location.LocationKind;
import com.openrecordsmanager.location.Location;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

import java.util.Map;
import java.util.UUID;

public record LocationResponse(
        @NotBlank UUID id,
        @NotBlank ResourceIdentifier type,
        @NotBlank String name,
        @NotNull LocationKind kind,
        @NotNull Map<String, @Nullable JsonNode> properties
) {
    public static LocationResponse of(Location location) {
        return new LocationResponse(
                location.getId(),
                location.getType().getId(),
                location.getName(),
                location.getKind(),
                location.toWireMap()
        );
    }
}