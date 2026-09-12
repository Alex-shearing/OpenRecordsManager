package com.openrecordsmanager.user.dto;

import com.openrecordsmanager.user.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

import java.util.Map;
import java.util.UUID;

public record UserResponse(
        @NotBlank UUID id,
        @NotBlank String username,
        @NotNull boolean enabled,
        @NotNull Map<String, JsonNode> properties
) {
    public static UserResponse of(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.isEnabled(),
                user.toWireMap()
        );
    }
}
