package com.openrecordsmanager.location.group.dto;

import com.openrecordsmanager.location.group.Group;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

import java.util.Map;
import java.util.UUID;

public record GroupResponse(
        @NotBlank UUID id,
        @NotBlank String name,
        @NotNull Map<String, JsonNode> properties
) {
    public static GroupResponse of(Group group) {
        return new GroupResponse(
                group.getId(),
                group.getName(),
                group.toWireMap()
        );
    }
}
