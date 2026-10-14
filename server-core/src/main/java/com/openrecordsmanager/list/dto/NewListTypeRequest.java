package com.openrecordsmanager.list.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NewListTypeRequest(
        @NotNull ResourceIdentifier id,
        @NotBlank String name
) {
}
