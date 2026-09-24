package com.openrecordsmanager.list.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.list.ListType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NonNull;

import java.time.Instant;
import java.util.List;

public record ListTypeResponse(
        @NonNull ResourceIdentifier type,
        @NonNull String name,
        @NotNull Instant dateCreated,
        @NotNull Instant dateModified,
        @NotBlank List<ListElementResponse> elements
) {
    public static ListTypeResponse of(ListType listType) {
        return new ListTypeResponse(
                listType.getId(),
                listType.getName(),
                listType.getDateCreated(),
                listType.getDateModified(),
                listType.getChildren().stream()
                        .map(ListElementResponse::of)
                        .toList()
        );
    }
}
