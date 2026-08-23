package com.openrecordsmanager.record.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record NewRecordRequest(
        @NotBlank ResourceIdentifier type,
        @NotNull Map<ResourceIdentifier, Object> properties
) {
}
