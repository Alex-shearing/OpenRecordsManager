package com.openrecordsmanager.controllers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"success", "errorCode", "timestamp", "data"})
public record ApiResponse<T>(boolean success,
                             T data,
                             String errorCode,
                             Instant timestamp) {

    // Convenience constructor for successful responses
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, Instant.now());
    }

    // Convenience constructor for error responses
    public static <T> ApiResponse<T> error(String errorCode) {
        return new ApiResponse<>(false, null, errorCode, Instant.now());
    }
}
