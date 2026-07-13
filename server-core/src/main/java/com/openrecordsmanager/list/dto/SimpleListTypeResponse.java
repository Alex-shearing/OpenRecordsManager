package com.openrecordsmanager.list.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import jakarta.validation.constraints.NotBlank;

public record SimpleListTypeResponse(@NotBlank ResourceIdentifier id, @NotBlank String name) {
}
