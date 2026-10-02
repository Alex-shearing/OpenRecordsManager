package com.openrecordsmanager.location.group.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

import java.util.Map;

public record NewGroupRequest(
        @NotBlank String name,
        @NotNull Map<ResourceIdentifier, JsonNode> properties
) {
}
