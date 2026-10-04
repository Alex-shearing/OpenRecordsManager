package com.openrecordsmanager.location.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.location.LocationKind;
import com.openrecordsmanager.location.Location;
import com.openrecordsmanager.location.user.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

import java.util.Map;
import java.util.UUID;

public record LocationResponse(
        @NotBlank UUID id,
        @NotBlank ResourceIdentifier type,
        @NotBlank String name,
        @NotBlank String displayName,
        @NotNull LocationKind kind,
        @Nullable UUID authProvider,
        @Nullable Boolean enabled,
        @NotNull Map<String, @Nullable JsonNode> properties
) {
    public static LocationResponse of(Location location) {
        if (location instanceof User user) {
            return new LocationResponse(
                    user.getId(),
                    user.getType().getId(),
                    user.getName(),
                    user.getDisplayName(),
                    user.getKind(),
                    user.getAuthProvider() != null ? user.getAuthProvider().getId() : null,
                    user.isEnabled(),
                    user.toWireMap()
            );
        }
        return new LocationResponse(
                location.getId(),
                location.getType().getId(),
                location.getName(),
                location.getDisplayName(),
                location.getKind(),
                null,
                null,
                location.toWireMap()
        );
    }
}
