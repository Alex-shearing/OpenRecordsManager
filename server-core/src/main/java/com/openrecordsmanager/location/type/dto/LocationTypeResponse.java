package com.openrecordsmanager.location.type.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.location.LocationKind;
import com.openrecordsmanager.location.type.LocationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

public record LocationTypeResponse(
        @NotBlank ResourceIdentifier id,
        @NotNull LocationKind kind,
        @NotNull Set<LocationTypePropertyResponse> properties,
        @NotNull Instant dateCreated,
        @NotNull Instant dateModified
) {
    public static LocationTypeResponse of(LocationType locationType) {
        return new LocationTypeResponse(
                locationType.getId(),
                locationType.getKind(),
                locationType.getProperties().stream()
                        .filter(property -> !property.getProperty().isUserHidden())
                        .map(LocationTypePropertyResponse::of)
                        .collect(Collectors.toSet()),
                locationType.getDateCreated(),
                locationType.getDateModified()
        );
    }
}
