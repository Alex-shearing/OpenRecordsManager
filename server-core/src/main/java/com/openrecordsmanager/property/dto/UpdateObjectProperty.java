package com.openrecordsmanager.property.dto;

import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;

public record UpdateObjectProperty(
        @NotBlank String name,
        @NotBlank String description,
        @Nullable String validator,
        @Nullable String securityFilter,
        @Nullable Object defaultValue,
        @NotBlank boolean userHidden
) {
}
