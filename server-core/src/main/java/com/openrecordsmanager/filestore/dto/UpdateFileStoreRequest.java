package com.openrecordsmanager.filestore.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record UpdateFileStoreRequest(
        @NotNull Map<String, ?> properties,
        @NotNull List<UUID> middlewares
) {
}
