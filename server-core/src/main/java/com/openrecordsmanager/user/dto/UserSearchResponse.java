package com.openrecordsmanager.user.dto;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public record UserSearchResponse(
        List<UserResponse> items,
        @Nullable UUID nextCursor
) {
}
