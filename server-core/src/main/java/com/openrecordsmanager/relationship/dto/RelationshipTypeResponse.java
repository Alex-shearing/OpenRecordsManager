package com.openrecordsmanager.relationship.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.property.ManagedObjectKind;
import com.openrecordsmanager.relationship.RelationshipType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

public record RelationshipTypeResponse(
        @NotBlank ResourceIdentifier id,
        @Nullable ManagedObjectKind sourceKind,
        @Nullable ManagedObjectKind targetKind,
        @NotNull ResourceIdentifier sourceTypeId,
        @NotNull ResourceIdentifier targetTypeId,
        @NotNull boolean uniquePerSource,
        @NotNull Instant dateCreated,
        @NotNull Instant dateModified
) {
    public static RelationshipTypeResponse of(RelationshipType type) {
        return new RelationshipTypeResponse(
                type.getId(),
                type.getSourceKind(),
                type.getTargetKind(),
                type.getSourceTypeId(),
                type.getTargetTypeId(),
                type.isUniquePerSource(),
                type.getDateCreated(),
                type.getDateModified()
        );
    }
}
