package com.openrecordsmanager.setup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompleteSetupRequest(
        @NotBlank @Size(min = 1, max = 128) String username,
        @NotBlank @Size(min = 8, max = 256) String password
) {
}
