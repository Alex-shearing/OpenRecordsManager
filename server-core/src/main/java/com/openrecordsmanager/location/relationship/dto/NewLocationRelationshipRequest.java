package com.openrecordsmanager.location.relationship.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

public record NewLocationRelationshipRequest(
        @NotNull UUID targetId,
        @NotNull ResourceIdentifier typeId,
        @Nullable Instant activeTo
) {
}
