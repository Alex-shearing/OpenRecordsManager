package com.openrecordsmanager.list.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.Date;
import java.util.Set;

public record UpdateListElement(
        @NotBlank String name,
        @NotBlank String description,
        @NotBlank int index,
        @Nullable Date activeTo,
        @NotNull Set<String> aliases
) {
}
