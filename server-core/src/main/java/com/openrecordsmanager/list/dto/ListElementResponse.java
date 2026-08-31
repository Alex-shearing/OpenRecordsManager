package com.openrecordsmanager.list.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.list.ListElement;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Set;

public record ListElementResponse(
        @NotBlank ResourceIdentifier type,
        @NotBlank String name,
        @NotBlank String description,
        @NotNull Set<String> aliases,
        @NotNull int index,
        @Nullable Instant activeTo) {


    public static ListElementResponse of(ListElement element) {
        return new ListElementResponse(
                element.getId(),
                element.getName(),
                element.getDescription(),
                element.getAliases(),
                element.getElementIndex(),
                element.getActiveTo()
        );
    }
}
