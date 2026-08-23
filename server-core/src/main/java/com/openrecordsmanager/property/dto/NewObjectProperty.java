package com.openrecordsmanager.property.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.property.PropertyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

public record NewObjectProperty(
        @NotBlank ResourceIdentifier id,
        @NotBlank String name,
        @NotBlank String description,
        @NotNull PropertyType<?> type,
        @Nullable ResourceIdentifier listType,
        @Nullable String validator,
        @Nullable String securityFilter,
        @Nullable Object defaultValue,
        @NotBlank boolean userHidden
) {
}
