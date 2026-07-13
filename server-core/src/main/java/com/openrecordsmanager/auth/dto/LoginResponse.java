package com.openrecordsmanager.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginResponse(@NotBlank String token, @NotBlank long expiresIn) {
}
