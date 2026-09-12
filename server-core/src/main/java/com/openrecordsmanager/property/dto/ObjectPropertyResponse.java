package com.openrecordsmanager.property.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.list.ListType;
import com.openrecordsmanager.list.dto.SimpleListTypeResponse;
import com.openrecordsmanager.property.ObjectProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

public record ObjectPropertyResponse(
        @NotBlank ResourceIdentifier id,
        @NotBlank String name,
        @NotBlank String description,
        @NotNull String type,
        @Nullable SimpleListTypeResponse listType,
        @Nullable String validator,
        @Nullable String securityFilter,
        @Nullable JsonNode defaultValue,
        boolean userHidden
) {
    public static ObjectPropertyResponse of(ObjectProperty<?> property) {
        ListType listType = property.getListType();
        return new ObjectPropertyResponse(
                property.getId(),
                property.getName(),
                property.getDescription(),
                property.getType().toString(),
                listType != null ? SimpleListTypeResponse.of(listType) : null,
                property.getValidator(),
                property.getSecurityFilter(),
                property.getDefaultValue(),
                property.isUserHidden()
        );
    }
}
