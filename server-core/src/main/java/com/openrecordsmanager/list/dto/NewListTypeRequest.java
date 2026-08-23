package com.openrecordsmanager.list.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import jakarta.validation.constraints.NotBlank;

public record NewListTypeRequest(
        @NotBlank ResourceIdentifier id,
        @NotBlank String name
) {
}
