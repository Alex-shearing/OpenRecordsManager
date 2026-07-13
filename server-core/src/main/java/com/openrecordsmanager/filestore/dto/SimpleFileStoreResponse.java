package com.openrecordsmanager.filestore.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record SimpleFileStoreResponse(
        @NotBlank UUID id,
        @NotBlank ResourceIdentifier type) {
}
