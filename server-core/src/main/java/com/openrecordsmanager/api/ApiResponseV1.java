package com.openrecordsmanager.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"success", "error", "timestamp", "data"})
public record ApiResponseV1<T extends @Nullable Object>(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean success,
        @Nullable T data,
        @Nullable String error,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant timestamp) {

    // Convenience constructor for successful responses
    public static <T extends @Nullable Object> ApiResponseV1<T> success(@Nullable T data) {
        return new ApiResponseV1<>(true, data, null, Instant.now());
    }

    // Convenience constructor for error responses
    public static <T> ApiResponseV1<T> error(String error) {
        return new ApiResponseV1<>(false, null, error, Instant.now());
    }

    @Override
    public String toString() {
        return "ApiResponseWrapper{" +
                "success=" + success +
                ", data=" + data +
                ", error='" + error + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
