package com.openrecordsmanager.filestore.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record NewFileStoreMiddlewareRequest(
        @NotBlank ResourceIdentifier type,
        @NotNull Map<String, ?> properties) {
}
