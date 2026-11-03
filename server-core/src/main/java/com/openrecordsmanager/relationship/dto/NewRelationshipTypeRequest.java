package com.openrecordsmanager.relationship.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import jakarta.validation.constraints.NotNull;

public record NewRelationshipTypeRequest(
        @NotNull ResourceIdentifier id,
        @NotNull ResourceIdentifier sourceTypeId,
        @NotNull ResourceIdentifier targetTypeId,
        boolean uniquePerSource
) {
}
