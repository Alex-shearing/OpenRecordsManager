package com.openrecordsmanager.auth.dto;

import com.openrecordsmanager.rest.dto.ComponentReferenceDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record NewAuthProviderRequest(
        @NotBlank String name,
        @NotNull ComponentReferenceDto type,
        @NotNull Map<String, Object> settings
) {
}
