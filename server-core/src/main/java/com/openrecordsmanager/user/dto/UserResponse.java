package com.openrecordsmanager.user.dto;

import com.openrecordsmanager.user.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record UserResponse(
        @NotBlank UUID id,
        @NotBlank String username,
        @NotNull boolean enabled,
        @NotBlank Map<String, Object> properties
) {
    public static UserResponse of(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.isEnabled(),
                user.toPropertyMap(true)
        );
    }
}
