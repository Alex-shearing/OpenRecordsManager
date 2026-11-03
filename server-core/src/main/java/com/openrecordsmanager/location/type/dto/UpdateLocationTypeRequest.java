package com.openrecordsmanager.location.type.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.property.ManagedObjectKind;
import com.openrecordsmanager.property.dto.TypePropertyAssignment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

public record UpdateLocationTypeRequest(
        @Nullable ManagedObjectKind kind,
        @NotNull boolean abstractType,
        @Nullable ResourceIdentifier baseTypeId,
        @NotNull @Valid List<TypePropertyAssignment> properties
) {
}
