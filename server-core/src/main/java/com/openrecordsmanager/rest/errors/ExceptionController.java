package com.openrecordsmanager.rest.errors;

import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.errors.InputValidationException;
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

@RestControllerAdvice
public class ExceptionController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionController.class);

    private final ConfigService config;

    public ExceptionController(ConfigService config) {
        this.config = config;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseV1<Void>> handleGeneralException(Exception ex) {
        HttpStatusCode httpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = this.config.getOrThrow(BuiltinConfigs.DEBUG_DETAILED_ERRORS) ? ex.getMessage() : "Internal Server Error";

        LOGGER.error("Unexpected error encountered while processing request", ex);

        return ResponseEntity.status(httpStatusCode)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponseV1.error(message));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponseV1<Void>> handleAuth(AuthenticationException ex) {
        String message = config.getOrThrow(BuiltinConfigs.DEBUG_DETAILED_ERRORS) ? ex.getMessage() : "Authentication Failed";

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponseV1.error(message));
    }

    @ExceptionHandler(ResourceInUseException.class)
    public ResponseEntity<ApiResponseV1<Void>> inUse(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponseV1.error(ex.getMessage()));
    }

    @ExceptionHandler(InputValidationException.class)
    public ResponseEntity<ApiResponseV1<ApiResponseV1<Void>>> inputValidation(InputValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponseV1.error("Validation failed", ex.getFieldErrors()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponseV1<Void>> accessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponseV1.error(ex.getMessage()));
    }

    @ExceptionHandler(AuditCommentRequiredException.class)
    public ResponseEntity<ApiResponseV1<Void>> auditCommentRequired(AuditCommentRequiredException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponseV1.error("audit_comment_required", ex.getMessage()));
    }
}
