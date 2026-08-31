package com.openrecordsmanager.auth.dto;

import org.jspecify.annotations.Nullable;

import java.util.Map;

public record UpdateAuthProviderRequest(
        @Nullable String name,
        @Nullable Boolean enabled,
        @Nullable Map<String, Object> settings
) {
}
