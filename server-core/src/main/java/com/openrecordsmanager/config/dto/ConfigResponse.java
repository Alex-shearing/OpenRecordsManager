package com.openrecordsmanager.config.dto;

import com.openrecordsmanager.config.ConfigItem;
import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

public record ConfigResponse(@NotBlank String key, @Nullable JsonNode value) {
    public static ConfigResponse of(ConfigItem item) {
        return new ConfigResponse(item.getKey(), item.getValue());
    }
}
