package com.openrecordsmanager.list.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.list.ListType;
import jakarta.validation.constraints.NotBlank;

public record SimpleListTypeResponse(@NotBlank ResourceIdentifier id, @NotBlank String name) {
    public static SimpleListTypeResponse of(ListType listType) {
        return new SimpleListTypeResponse(listType.id, listType.name);
    }
}
