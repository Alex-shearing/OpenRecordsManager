package com.openrecordsmanager.auth.dto;

import com.openrecordsmanager.auth.entity.AuthToken;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record LoginResponse(@NotBlank String token, @NotBlank Instant expires) {
    public static LoginResponse of(AuthToken token) {
        return new LoginResponse(token.getToken(), token.getExpiryDate());
    }
}
