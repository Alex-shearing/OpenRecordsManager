package com.openrecordsmanager.list.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.list.ListType;
import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.NonNull;

import java.util.List;

public record ListTypeResponse(
        @NonNull ResourceIdentifier type,
        @NonNull String name,
        @NotBlank List<ListElementResponse> elements
) {
    public static ListTypeResponse of(ListType listType) {
        return new ListTypeResponse(
                listType.getId(),
                listType.getName(),
                listType.getChildren().stream()
                        .map(ListElementResponse::of)
                        .toList()
        );
    }
}
