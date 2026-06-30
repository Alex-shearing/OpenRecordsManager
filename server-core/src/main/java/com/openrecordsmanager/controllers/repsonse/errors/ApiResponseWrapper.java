package com.openrecordsmanager.controllers.repsonse.errors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"success", "error", "timestamp", "data"})
public record ApiResponseWrapper<T>(boolean success,
                                    T data,
                                    String error,
                                    Instant timestamp) {

    // Convenience constructor for successful responses
    public static <T> ApiResponseWrapper<T> success(T data) {
        return new ApiResponseWrapper<>(true, data, null, Instant.now());
    }

    // Convenience constructor for error responses
    public static <T> ApiResponseWrapper<T> error(String error) {
        return new ApiResponseWrapper<>(false, null, error, Instant.now());
    }
}
