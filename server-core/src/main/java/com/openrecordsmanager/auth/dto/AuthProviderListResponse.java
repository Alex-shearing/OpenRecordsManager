package com.openrecordsmanager.auth.dto;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.plugin.types.ComponentReferenceSerializer;
import io.swagger.v3.oas.annotations.StringToClassMapItem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.annotation.JsonSerialize;

import java.util.Map;
import java.util.UUID;

public record AuthProviderListResponse(
        @NotBlank UUID id,
        @JsonSerialize(using = ComponentReferenceSerializer.class)
        @Schema(
                type = "object",
                implementation = Map.class,
                properties = {
                        @StringToClassMapItem(key = "id", value = String.class),
                        @StringToClassMapItem(key = "type", value = String.class),
                },
                requiredProperties = {"id", "type"}
        )
        @NotNull ComponentReference<?> type
) {
}
