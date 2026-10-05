package com.openrecordsmanager.record.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public record RecordSearchResponse(
        List<RecordResponse> items,
        @Nullable UUID nextCursor,
        List<ResourceIdentifier> columns
) {
}