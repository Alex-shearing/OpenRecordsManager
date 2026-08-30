package com.openrecordsmanager.user.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public record NewUserRequest(
        @NotBlank String username,
        @Nullable UUID authProvider,
        @NotNull Map<ResourceIdentifier, Object> properties
) {
}
