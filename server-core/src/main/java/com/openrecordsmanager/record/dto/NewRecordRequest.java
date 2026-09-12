package com.openrecordsmanager.record.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

import java.util.Map;

public record NewRecordRequest(
        @NotBlank ResourceIdentifier type,
        @NotNull
        @Schema(description = "Property id → JSON value", additionalProperties = Schema.AdditionalPropertiesValue.TRUE)
        Map<ResourceIdentifier, JsonNode> properties
) {
}
