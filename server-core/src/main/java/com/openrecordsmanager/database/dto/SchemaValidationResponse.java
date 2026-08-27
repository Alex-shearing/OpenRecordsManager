package com.openrecordsmanager.database.dto;

import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;

public record SchemaValidationResponse(
        @NotBlank boolean validated,
        @Nullable String message) {
}
