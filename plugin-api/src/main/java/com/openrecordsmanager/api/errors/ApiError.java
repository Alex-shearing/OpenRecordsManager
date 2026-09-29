package com.openrecordsmanager.api.errors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;
import java.util.Objects;

/**
 * Machine-readable error detail: catalog key under {@code error.*} plus interpolation args.
 * Serialized as {@code error} / {@code errorArgs} to match the API envelope.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({"error", "errorArgs"})
public record ApiError(
        @JsonProperty("error") String code,
        @JsonProperty("errorArgs") List<String> args
) {
    public ApiError {
        Objects.requireNonNull(code, "code");
        if (code.isBlank()) {
            throw new IllegalArgumentException("error code is required");
        }
        args = List.copyOf(Objects.requireNonNull(args, "args"));
    }

    public static ApiError of(String code) {
        return new ApiError(code, List.of());
    }

    public static ApiError of(String code, String... args) {
        return new ApiError(code, List.of(args));
    }

    public static ApiError of(String code, List<String> args) {
        return new ApiError(code, args);
    }
}
