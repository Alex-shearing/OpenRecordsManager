package com.openrecordsmanager.user.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record UserResponse(@NotBlank String username, @NotBlank Map<String, Object> properties) {
}
