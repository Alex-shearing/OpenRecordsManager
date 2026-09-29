package com.openrecordsmanager.rest.exception;

import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.errors.ApiException;
import com.openrecordsmanager.config.ConfigService;
import com.openrecordsmanager.rest.dto.ApiResponseV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class ExceptionController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionController.class);

    private final ConfigService config;

    public ExceptionController(ConfigService config) {
        this.config = config;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponseV1<Void>> handleApiException(ApiException ex) {
        HttpStatus status = switch (ex) {
            case ResourceNotFoundException _ -> HttpStatus.NOT_FOUND;
            case ResourceInUseException _ -> HttpStatus.CONFLICT;
            case ForbiddenException _ -> HttpStatus.FORBIDDEN;
            case AuditCommentRequiredException _ -> HttpStatus.UNPROCESSABLE_CONTENT;
            default -> HttpStatus.BAD_REQUEST;
        };

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponseV1.error(ex.getCode(), ex.getArgs(), ex.getFieldErrors()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseV1<Void>> handleGeneralException(Exception ex) {
        HttpStatusCode httpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR;
        LOGGER.error("Unexpected error encountered while processing request", ex);

        boolean detailed = this.config.getOrDefault(BuiltinConfigs.DEBUG_DETAILED_ERRORS, false);
        if (detailed && ex.getMessage() != null) {
            return ResponseEntity.status(httpStatusCode)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponseV1.error("internal_server_error_detailed", List.of(ex.getMessage())));
        }

        return ResponseEntity.status(httpStatusCode)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponseV1.error("internal_server_error"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponseV1<Void>> handleAuth(AuthenticationException ex) {
        boolean detailed = this.config.getOrDefault(BuiltinConfigs.DEBUG_DETAILED_ERRORS, false);
        if (detailed && ex.getMessage() != null && !ex.getMessage().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponseV1.error("authentication_failed_detailed", List.of(ex.getMessage())));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponseV1.error("authentication_failed"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponseV1<Void>> accessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponseV1.error("access_denied"));
    }
}
