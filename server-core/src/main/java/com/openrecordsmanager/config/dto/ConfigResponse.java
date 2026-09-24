package com.openrecordsmanager.config.dto;

import com.openrecordsmanager.config.ConfigItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

import java.time.Instant;

public record ConfigResponse(
        @NotBlank String key,
        @Nullable JsonNode value,
        @NotNull Instant dateModified
) {
    public static ConfigResponse of(ConfigItem item) {
        return new ConfigResponse(
                item.getKey(),
                item.getValue(),
                item.getDateModified()
        );
    }
}
