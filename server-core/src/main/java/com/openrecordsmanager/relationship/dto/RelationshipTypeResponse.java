package com.openrecordsmanager.relationship.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.relationship.RelationshipType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record RelationshipTypeResponse(
        @NotBlank ResourceIdentifier id,
        @NotNull ResourceIdentifier sourceTypeId,
        @NotNull ResourceIdentifier targetTypeId,
        @NotNull boolean uniquePerSource,
        @NotNull Instant dateCreated,
        @NotNull Instant dateModified
) {
    public static RelationshipTypeResponse of(RelationshipType type) {
        return new RelationshipTypeResponse(
                type.getId(),
                type.getSourceTypeId(),
                type.getTargetTypeId(),
                type.isUniquePerSource(),
                type.getDateCreated(),
                type.getDateModified()
        );
    }
}
