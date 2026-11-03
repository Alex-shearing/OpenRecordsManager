package com.openrecordsmanager.location.type.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.property.ManagedObjectKind;
import com.openrecordsmanager.location.type.LocationType;
import com.openrecordsmanager.template.dto.ManagedObjectTypePropertyResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

public record LocationTypeResponse(
        @NotBlank ResourceIdentifier id,
        @Nullable ManagedObjectKind kind,
        @NotNull boolean abstractType,
        @Nullable ResourceIdentifier baseTypeId,
        @NotNull Set<ManagedObjectTypePropertyResponse> properties,
        @NotNull Instant dateCreated,
        @NotNull Instant dateModified
) {
    public static LocationTypeResponse of(LocationType locationType) {
        return new LocationTypeResponse(
                locationType.getId(),
                locationType.getKind(),
                locationType.isAbstract(),
                locationType.getBaseTypeId(),
                locationType.getDeclaredProperties().stream()
                        .filter(property -> !property.getProperty().isSystemProperty())
                        .map(ManagedObjectTypePropertyResponse::of)
                        .collect(Collectors.toSet()),
                locationType.getDateCreated(),
                locationType.getDateModified()
        );
    }
}
