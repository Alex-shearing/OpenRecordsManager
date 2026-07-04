package com.openrecordsmanager.controllers.repsonse.errors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"success", "error", "timestamp", "data"})
public record ApiResponseWrapper<T>(boolean success,
                                    @Nullable T data,
                                    @Nullable String error,
                                    Instant timestamp) {

    // Convenience constructor for successful responses
    public static <T> ApiResponseWrapper<T> success(@Nullable T data) {
        T typedData = data;
        return new ApiResponseWrapper<>(true, typedData, null, Instant.now());
    }

    // Convenience constructor for error responses
    public static <T> ApiResponseWrapper<T> error(String error) {
        return new ApiResponseWrapper<>(false, null, error, Instant.now());
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
