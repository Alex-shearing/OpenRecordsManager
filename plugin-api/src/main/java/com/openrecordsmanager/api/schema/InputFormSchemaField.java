package com.openrecordsmanager.api.schema;

import com.networknt.schema.Schema;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

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
