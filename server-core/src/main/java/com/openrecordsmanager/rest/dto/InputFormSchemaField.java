package com.openrecordsmanager.rest.dto;

import org.jspecify.annotations.Nullable;

public record InputFormSchemaField(
        @Nullable String type,
        @Nullable String title,
        @Nullable String description,
        @Nullable Boolean writeOnly,
        @Nullable String format,
        @Nullable Integer minLength,
        @Nullable Integer maxLength,
        @Nullable String pattern,
        @Nullable String contentEncoding
) {
}
