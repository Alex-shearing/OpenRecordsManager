package com.openrecordsmanager.relationship.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import org.jspecify.annotations.Nullable;

public record UpdateRelationshipTypeRequest(
        @Nullable ResourceIdentifier sourceTypeId,
        @Nullable ResourceIdentifier targetTypeId,
        @Nullable Boolean uniquePerSource
) {
}
