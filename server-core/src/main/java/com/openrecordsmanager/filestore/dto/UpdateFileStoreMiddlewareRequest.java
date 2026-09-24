package com.openrecordsmanager.filestore.dto;

import org.jspecify.annotations.Nullable;

import java.util.Map;

public record UpdateFileStoreMiddlewareRequest(
        @Nullable String name,
        @Nullable Map<String, ?> properties
) {
}
