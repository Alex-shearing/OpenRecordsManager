package com.openrecordsmanager.location.group.dto;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public record GroupSearchResponse(
        @NonNull List<GroupResponse> items,
        @Nullable UUID nextCursor
) {
}
