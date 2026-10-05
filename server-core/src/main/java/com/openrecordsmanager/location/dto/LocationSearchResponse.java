package com.openrecordsmanager.location.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public record LocationSearchResponse(
        List<LocationResponse> items,
        @Nullable UUID nextCursor,
        List<ResourceIdentifier> columns
) {
}
