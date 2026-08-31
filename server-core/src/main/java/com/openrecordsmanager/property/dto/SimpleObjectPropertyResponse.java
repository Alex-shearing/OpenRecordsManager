package com.openrecordsmanager.property.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.property.ObjectProperty;
import jakarta.validation.constraints.NotBlank;

public record SimpleObjectPropertyResponse(
        @NotBlank ResourceIdentifier id,
        @NotBlank String name,
        @NotBlank String type
) {
    public static SimpleObjectPropertyResponse of(ObjectProperty<?> property) {
        return new SimpleObjectPropertyResponse(
                property.getId(),
                property.getName(),
                property.getType().getName()
        );
    }
}
