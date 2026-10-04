package com.openrecordsmanager.location.type.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openrecordsmanager.location.type.LocationTypeProperty;
import com.openrecordsmanager.property.dto.ObjectPropertyResponse;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

public record LocationTypePropertyResponse(
        @NotNull ObjectPropertyResponse property,
        @JsonProperty("default") @Nullable JsonNode defaultValue
) {
    public static LocationTypePropertyResponse of(LocationTypeProperty<?> property) {
        return new LocationTypePropertyResponse(
                ObjectPropertyResponse.of(property.getProperty()),
                property.getDefault()
        );
    }
}
