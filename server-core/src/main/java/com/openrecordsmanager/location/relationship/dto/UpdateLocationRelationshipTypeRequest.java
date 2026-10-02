package com.openrecordsmanager.location.relationship.dto;

import com.openrecordsmanager.api.location.LocationKind;
import org.jspecify.annotations.Nullable;

public record UpdateLocationRelationshipTypeRequest(
        @Nullable LocationKind sourceKind,
        @Nullable LocationKind targetKind,
        @Nullable Boolean uniquePerSource
) {
}
