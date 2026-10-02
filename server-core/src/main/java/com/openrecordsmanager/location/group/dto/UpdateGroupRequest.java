package com.openrecordsmanager.location.group.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

import java.util.Map;

public record UpdateGroupRequest(
        @Nullable String name,
        @Nullable Map<ResourceIdentifier, JsonNode> properties
) {
}
