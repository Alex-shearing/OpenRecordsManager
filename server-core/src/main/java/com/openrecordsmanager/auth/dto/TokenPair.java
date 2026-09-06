package com.openrecordsmanager.auth.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record TokenPair(
        @NotBlank String accessToken,
        @NotBlank Instant accessExpires,
        @NotBlank String refreshToken,
        @NotBlank Instant refreshExpires,
        @NotBlank SessionMode sessionMode
) {
}
