package com.openrecordsmanager.property.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openrecordsmanager.api.ResourceIdentifier;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

public record TypePropertyAssignment(
        @NotNull ResourceIdentifier property,
        @JsonProperty("default") @Nullable JsonNode defaultValue
) {
}
