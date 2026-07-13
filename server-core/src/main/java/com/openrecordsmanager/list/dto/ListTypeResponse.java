package com.openrecordsmanager.list.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.NonNull;

import java.util.List;

public record ListTypeResponse(
        @NonNull ResourceIdentifier type,
        @NonNull String name,
        @NotBlank List<ListElementResponse> elements) {
}
