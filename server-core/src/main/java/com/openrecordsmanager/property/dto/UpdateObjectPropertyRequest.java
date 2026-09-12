package com.openrecordsmanager.property.dto;

import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

public record UpdateObjectPropertyRequest(
        @NotBlank String name,
        @NotBlank String description,
        @Nullable String validator,
        @Nullable String securityFilter,
        @Nullable JsonNode defaultValue,
        @NotBlank boolean userHidden
) {
}
