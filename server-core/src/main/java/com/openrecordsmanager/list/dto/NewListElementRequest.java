package com.openrecordsmanager.list.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.Date;
import java.util.Set;

public record NewListElementRequest(
        @NotBlank ResourceIdentifier id,
        @NotBlank String name,
        @NotBlank String description,
        int index,
        @Nullable Date activeTo,
        @NotNull Set<String> aliases
) {
}
