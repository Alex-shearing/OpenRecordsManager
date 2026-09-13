package com.openrecordsmanager.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;

import java.util.List;

public record InputFormSchemaField(
        @NotBlank String type,
        @NotBlank String title,
        @Nullable String description,
        @Nullable Boolean writeOnly,
        @Nullable String format,
        @Nullable Integer minLength,
        @Nullable Integer maxLength,
        @Nullable String pattern,
        @Nullable String contentEncoding,
        @Nullable @JsonProperty("enum") List<String> enumValues
) {
}
