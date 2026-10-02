package com.openrecordsmanager.location.relationship.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.location.LocationKind;
import com.openrecordsmanager.location.relationship.LocationRelationshipType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LocationRelationshipTypeResponse(
        @NotBlank ResourceIdentifier id,
        @NotNull LocationKind sourceKind,
        @NotNull LocationKind targetKind,
        @NotNull boolean uniquePerSource
) {
    public static LocationRelationshipTypeResponse of(LocationRelationshipType type) {
        return new LocationRelationshipTypeResponse(
                type.getId(),
                type.getSourceKind(),
                type.getTargetKind(),
                type.isUniquePerSource()
        );
    }
}
