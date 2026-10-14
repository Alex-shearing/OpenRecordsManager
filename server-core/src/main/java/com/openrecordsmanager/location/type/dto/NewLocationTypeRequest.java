package com.openrecordsmanager.location.type.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.location.LocationKind;
import com.openrecordsmanager.property.dto.TypePropertyAssignment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record NewLocationTypeRequest(
        @NotNull ResourceIdentifier id,
        @NotNull LocationKind kind,
        @NotNull @Valid List<TypePropertyAssignment> properties
) {
}
