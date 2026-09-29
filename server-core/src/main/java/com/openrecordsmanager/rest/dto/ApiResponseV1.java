package com.openrecordsmanager.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.openrecordsmanager.api.errors.ApiError;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"success", "error", "errorArgs", "fieldErrors", "timestamp", "data"})
public record ApiResponseV1<T extends @Nullable Object>(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean success,
        @Nullable T data,
        @Nullable String error,
        @Nullable List<String> errorArgs,
        @Nullable Map<String, ApiError> fieldErrors,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant timestamp) {

    public static <T extends @Nullable Object> ApiResponseV1<T> success(@Nullable T data) {
        return new ApiResponseV1<>(true, data, null, null, null, Instant.now());
    }

    public static <T> ApiResponseV1<T> error(String error) {
        return error(error, List.of(), Map.of());
    }

    public static <T> ApiResponseV1<T> error(String error, List<String> errorArgs) {
        return error(error, errorArgs, Map.of());
    }

    public static <T> ApiResponseV1<T> error(String error, List<String> errorArgs, Map<String, ApiError> fieldErrors) {
        return new ApiResponseV1<>(
                false,
                null,
                error,
                errorArgs.isEmpty() ? null : List.copyOf(errorArgs),
                fieldErrors.isEmpty() ? null : Map.copyOf(fieldErrors),
                Instant.now()
        );
    }

    @Override
    public String toString() {
        return "ApiResponseV1{" +
                "success=" + success +
                ", data=" + data +
                ", error='" + error + '\'' +
                ", errorArgs=" + errorArgs +
                ", fieldErrors=" + fieldErrors +
                ", timestamp=" + timestamp +
                '}';
    }
}
