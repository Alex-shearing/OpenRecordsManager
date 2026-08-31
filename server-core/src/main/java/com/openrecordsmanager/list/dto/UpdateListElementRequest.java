package com.openrecordsmanager.list.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Set;

public record UpdateListElementRequest(
        @NotBlank String name,
        @NotBlank String description,
        @NotBlank int index,
        @Nullable Instant activeTo,
        @NotNull Set<String> aliases
) {
}
