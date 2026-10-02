package com.openrecordsmanager.location.relationship.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.location.relationship.LocationRelationship;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

public record LocationRelationshipResponse(
        @NotBlank UUID id,
        @NotBlank UUID sourceId,
        @NotBlank UUID targetId,
        @NotBlank ResourceIdentifier typeId,
        @NotNull Instant dateCreated,
        @Nullable Instant activeTo
) {
    public static LocationRelationshipResponse of(LocationRelationship relationship) {
        return new LocationRelationshipResponse(
                relationship.getId(),
                relationship.getSource().getId(),
                relationship.getTarget().getId(),
                relationship.getType().getId(),
                relationship.getDateCreated(),
                relationship.getActiveTo()
        );
    }
}
