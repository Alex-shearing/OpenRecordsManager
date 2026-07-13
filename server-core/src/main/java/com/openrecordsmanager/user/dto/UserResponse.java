package com.openrecordsmanager.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UserResponse(@NotBlank String username) {
}
