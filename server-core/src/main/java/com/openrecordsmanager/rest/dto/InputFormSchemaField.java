package com.openrecordsmanager.rest.dto;

import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;

public record InputFormSchemaField(
        @NotBlank String type,
        @NotBlank String title,
        @Nullable String description,
        @Nullable Boolean writeOnly,
        @Nullable String format,
        @Nullable Integer minLength,
        @Nullable Integer maxLength,
        @Nullable String pattern,
        @Nullable String contentEncoding
) {
}
