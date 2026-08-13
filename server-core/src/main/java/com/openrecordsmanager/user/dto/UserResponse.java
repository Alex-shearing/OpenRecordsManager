package com.openrecordsmanager.user.dto;

import com.openrecordsmanager.user.User;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;
import java.util.UUID;

public record UserResponse(@NotBlank UUID id, @NotBlank String username, @NotBlank Map<String, Object> properties) {
    public static UserResponse of(User user) {
        return new UserResponse(user.id, user.getUsername(), user.toPropertyMap(true));
    }
}
