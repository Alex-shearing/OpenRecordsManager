package com.openrecordsmanager.filestore.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.filestore.middleware.MiddlewareUsage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record FileStoreResponse(
        @NotBlank UUID id,
        @NotBlank ResourceIdentifier type,
        @NotNull Map<String, ?> properties,
        @NotNull List<MiddlewareUsage> middlewares) {
}
