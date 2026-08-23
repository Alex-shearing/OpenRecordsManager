package com.openrecordsmanager.property.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.property.PropertyType;
import com.openrecordsmanager.list.ListType;
import com.openrecordsmanager.list.dto.SimpleListTypeResponse;
import com.openrecordsmanager.property.ObjectProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

public record ObjectPropertyResponse(
        @NotBlank ResourceIdentifier id,
        @NotBlank String name,
        @NotBlank String description,
        @NotNull PropertyType<?> type,
        @Nullable SimpleListTypeResponse listType,
        @Nullable String validator,
        @Nullable String securityFilter,
        @Nullable Object defaultValue,
        boolean userHidden
) {
    public static ObjectPropertyResponse of(ObjectProperty<?> property) {
        ListType listType = property.getListType();
        return new ObjectPropertyResponse(
                property.getId(),
                property.getName(),
                property.getDescription(),
                property.getType(),
                listType != null ? new SimpleListTypeResponse(listType.id, listType.name) : null,
                property.getValidator(),
                property.getSecurityFilter(),
                property.getDefaultValue(),
                property.isUserHidden()
        );
    }
}
