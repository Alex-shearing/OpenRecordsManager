package com.openrecordsmanager.location.user.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.location.user.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

import java.util.Map;
import java.util.UUID;

public record UserResponse(
        @NotBlank UUID id,
        @NotBlank ResourceIdentifier type,
        @NotBlank String username,
        @Nullable UUID authProvider,
        @NotNull boolean enabled,
        @NotNull Map<String, @Nullable JsonNode> properties
) {
    public static UserResponse of(User user) {
        return new UserResponse(
                user.getId(),
                user.getType().getId(),
                user.getUsername(),
                user.getAuthProvider() != null ? user.getAuthProvider().getId() : null,
                user.isEnabled(),
                user.toWireMap()
        );
    }
}
