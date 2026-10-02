package com.openrecordsmanager.location.relationship.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

public record NewLocationRelationshipRequest(
        @NotBlank UUID targetId,
        @NotBlank ResourceIdentifier typeId,
        @Nullable Instant activeTo
) {
}
