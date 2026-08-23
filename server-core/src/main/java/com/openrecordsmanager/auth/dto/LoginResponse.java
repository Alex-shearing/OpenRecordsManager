package com.openrecordsmanager.auth.dto;

import com.openrecordsmanager.auth.entity.AuthToken;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record LoginResponse(@NotBlank String token, @NotBlank LocalDateTime expires) {
    public static LoginResponse of(AuthToken token) {
        return new LoginResponse(token.getToken(), token.getExpiryDate());
    }
}
