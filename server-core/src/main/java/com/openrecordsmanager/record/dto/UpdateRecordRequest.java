package com.openrecordsmanager.record.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public record UpdateRecordRequest(
        @Nullable ResourceIdentifier type,
        @Nullable Map<ResourceIdentifier, Object> properties
) {
}
