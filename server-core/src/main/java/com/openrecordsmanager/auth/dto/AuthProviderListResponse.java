package com.openrecordsmanager.auth.dto;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.plugin.types.ComponentReferenceSerializer;
import tools.jackson.databind.annotation.JsonSerialize;

import java.util.UUID;

public record AuthProviderListResponse(
        UUID id,
        @JsonSerialize(using = ComponentReferenceSerializer.class) ComponentReference<?> type
) {
}
