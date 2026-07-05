package com.openrecordsmanager.controllers.repsonse.errors;

import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.config.DynamicConfigService;
import com.openrecordsmanager.controllers.repsonse.ApiResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionController.class);

    private final DynamicConfigService config;

    public ExceptionController(DynamicConfigService config) {
        this.config = config;
    }

    @ExceptionHandler(Exception.class)
    @SuppressWarnings("unused")
    public ResponseEntity<ApiResponseWrapper<Void>> handleGeneralException(Exception ex) {
        HttpStatusCode httpStatusCode = ex instanceof ApiError apiError ? apiError.httpStatusCode : HttpStatus.INTERNAL_SERVER_ERROR;
        String message = config.getOrThrow(BuiltinConfigs.DEBUG_DETAILED_ERRORS) ? ex.getMessage() :
                ex instanceof ApiError error ? error.getUserMessage() : "Internal Server Error";

        if (!(ex instanceof ApiError.NotFound error) || error.shouldLog()) {
            LOGGER.error("Unexpected error encountered while processing request", ex);
        }

        return ResponseEntity.status(httpStatusCode)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponseWrapper.error(message));
    }
}
