package com.openrecordsmanager.location.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

import java.util.Map;
import java.util.UUID;

public record UpdateLocationRequest(
        @Nullable UUID authProvider,
        @Nullable Boolean enabled,
        @Nullable String name,
        @Nullable Map<ResourceIdentifier, JsonNode> properties
) {
}
