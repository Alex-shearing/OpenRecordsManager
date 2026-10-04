package com.openrecordsmanager.location.group.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.location.group.Group;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

import java.util.Map;
import java.util.UUID;

public record GroupResponse(
        @NotBlank UUID id,
        @NotBlank ResourceIdentifier type,
        @NotBlank String name,
        @NotNull Map<String, @Nullable JsonNode> properties
) {
    public static GroupResponse of(Group group) {
        return new GroupResponse(
                group.getId(),
                group.getType().getId(),
                group.getName(),
                group.toWireMap()
        );
    }
}
