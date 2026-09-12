package com.openrecordsmanager.property.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.property.PropertyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

public record NewObjectPropertyRequest(
        @NotBlank ResourceIdentifier id,
        @NotBlank String name,
        @NotBlank String description,
        @NotNull PropertyType<?> type,
        @Nullable ResourceIdentifier listType,
        @Nullable String validator,
        @Nullable String securityFilter,
        @Nullable JsonNode defaultValue,
        @NotBlank boolean userHidden
) {
}
