package com.openrecordsmanager.filestore.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record UpdateFileStoreRequest(
        @NotNull Map<String, ?> properties
) {
}
