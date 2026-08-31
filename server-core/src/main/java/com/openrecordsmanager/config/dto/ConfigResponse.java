package com.openrecordsmanager.config.dto;

import com.openrecordsmanager.config.ConfigItem;
import jakarta.validation.constraints.NotBlank;

public record ConfigResponse(@NotBlank String key, @NotBlank Object value) {
    public static ConfigResponse of(ConfigItem item) {
        return new ConfigResponse(item.getKey(), item.getValue());
    }
}
