package com.openrecordsmanager.location.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

import java.util.Map;
import java.util.UUID;

public record NewLocationRequest(
        @NotNull ResourceIdentifier type,
        @Nullable UUID authProvider,
        @Nullable String name,
        @Schema(description = "Property id to JSON value map", additionalProperties = Schema.AdditionalPropertiesValue.TRUE)
        @NotNull Map<ResourceIdentifier, JsonNode> properties
) {
}
