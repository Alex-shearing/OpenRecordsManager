package com.openrecordsmanager.rest.exception;

import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.errors.ApiError;
import com.openrecordsmanager.api.errors.ApiException;
import com.openrecordsmanager.config.ConfigService;
import com.openrecordsmanager.rest.dto.ApiResponseV1;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;

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

        return makeErrResponse(status, ex.getError(), ex.getFieldErrors());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponseV1<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String value = ex.getValue() != null ? String.valueOf(ex.getValue()) : "";
        return makeErrResponse(HttpStatus.BAD_REQUEST, ApiError.of("invalid_request_parameter", ex.getName(), value));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponseV1<Void>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return makeErrResponse(HttpStatus.BAD_REQUEST, ApiError.of("invalid_request_body"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseV1<Void>> handleGeneralException(Exception ex) {
        LOGGER.error("Unexpected error encountered while processing request", ex);

        boolean detailed = this.config.getOrDefault(BuiltinConfigs.DEBUG_DETAILED_ERRORS, false);

        ApiError error = detailed && !StringUtils.isBlank(ex.getMessage())
                ? ApiError.of("internal_server_error_detailed", List.of(ex.getMessage()))
                : ApiError.of("internal_server_error");

        return makeErrResponse(HttpStatus.INTERNAL_SERVER_ERROR, error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponseV1<Void>> handleAuth(AuthenticationException ex) {
        boolean detailed = this.config.getOrDefault(BuiltinConfigs.DEBUG_DETAILED_ERRORS, false);

        ApiError error = detailed && !StringUtils.isBlank(ex.getMessage())
                ? ApiError.of("authentication_failed_detailed", List.of(ex.getMessage()))
                : ApiError.of("authentication_failed");

        return makeErrResponse(HttpStatus.UNAUTHORIZED, error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponseV1<Void>> accessDenied(AccessDeniedException ex) {
        return makeErrResponse(HttpStatus.FORBIDDEN, ApiError.of("access_denied"));
    }

    private static ResponseEntity<ApiResponseV1<Void>> makeErrResponse(HttpStatus status, ApiError error) {
        return makeErrResponse(status, error, Map.of());
    }

    private static ResponseEntity<ApiResponseV1<Void>> makeErrResponse(
            HttpStatus status,
            ApiError error,
            Map<String, ApiError> fieldErrors
    ) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponseV1.error(error, fieldErrors));
    }
}
