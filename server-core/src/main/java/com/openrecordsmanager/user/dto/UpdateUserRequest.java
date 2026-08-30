package com.openrecordsmanager.user.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public record UpdateUserRequest(
        @Nullable String username,
        @Nullable UUID authProvider,
        @Nullable Map<ResourceIdentifier, Object> properties
) {
}
