package com.openrecordsmanager.user.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

import java.util.Map;
import java.util.UUID;

public record NewUserRequest(
        @NotBlank String username,
        @Nullable UUID authProvider,
        @NotNull
        @Schema(description = "Property id → JSON value", additionalProperties = Schema.AdditionalPropertiesValue.TRUE)
        Map<ResourceIdentifier, JsonNode> properties
) {
}
