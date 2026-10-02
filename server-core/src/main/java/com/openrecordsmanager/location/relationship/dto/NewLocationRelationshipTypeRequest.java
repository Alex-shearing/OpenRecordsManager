package com.openrecordsmanager.location.relationship.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.location.LocationKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NewLocationRelationshipTypeRequest(
        @NotBlank ResourceIdentifier id,
        @NotNull LocationKind sourceKind,
        @NotNull LocationKind targetKind,
        boolean uniquePerSource
) {
}
