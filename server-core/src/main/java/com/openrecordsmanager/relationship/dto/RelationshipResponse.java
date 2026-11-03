package com.openrecordsmanager.relationship.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.property.ManagedObjectKind;
import com.openrecordsmanager.relationship.Relationship;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

public record RelationshipResponse(
        @NotNull UUID id,
        @NotNull UUID sourceId,
        @NotNull UUID targetId,
        @NotNull ResourceIdentifier typeId,
        @NotNull ManagedObjectKind sourceKind,
        @NotNull ManagedObjectKind targetKind,
        @NotNull Instant dateCreated,
        @Nullable Instant activeTo
) {
    public static RelationshipResponse of(Relationship relationship) {
        return new RelationshipResponse(
                relationship.getId(),
                relationship.getSource().getId(),
                relationship.getTarget().getId(),
                relationship.getType().getId(),
                relationship.getSource().getKind(),
                relationship.getTarget().getKind(),
                relationship.getDateCreated(),
                relationship.getActiveTo()
        );
    }
}
