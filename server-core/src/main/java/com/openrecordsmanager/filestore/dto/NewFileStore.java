package com.openrecordsmanager.filestore.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record NewFileStore(
        @NotBlank ResourceIdentifier type,
        @NotNull Map<String, ?> properties,
        @NotNull List<UUID> middlewares) {
}
