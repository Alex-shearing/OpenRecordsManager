package com.openrecordsmanager.list.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Set;

public record NewListElementRequest(
        @NotNull ResourceIdentifier id,
        @NotBlank String name,
        @NotBlank String description,
        @NotNull Integer index,
        @Nullable Instant activeTo,
        @NotNull Set<String> aliases
) {
}
