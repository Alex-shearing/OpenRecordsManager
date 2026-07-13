package com.openrecordsmanager.config.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfigResponse(@NotBlank String key, @NotBlank Object value) {
}
