package com.openrecordsmanager.controllers.errors;

import com.openrecordsmanager.resources.ResourceIdentifier;
import com.openrecordsmanager.resources.types.ComponentType;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.text.MessageFormat;

public class ApiError extends RuntimeException {
    public final HttpStatusCode httpStatusCode;

    public ApiError(HttpStatusCode code, String message) {
        super(message);
        this.httpStatusCode = code;
    }

    public static ApiError notFound(String type, String resource) {
        return new NotFound(type, resource);
    }

    public static ApiError notFound(ComponentType<?, ?> type, ResourceIdentifier resource) {
        return ApiError.notFound(type.toString(), resource.toString());
    }

    public static ApiError templateNotFound(ComponentType<?, ?> type, ResourceIdentifier resource) {
        return ApiError.notFound(type.toString() + " template", resource.toString());
    }

    public static ApiError serverError(String pattern, Object... params) {
        return new ServerError(MessageFormat.format(pattern, params));
    }

    public static ApiError authError(String error) {
        return new AuthenticationError(error);
    }

    public boolean shouldLog() {
        return true;
    }

    public String getUserMessage() {
        return "Internal Server Error";
    }

    public static class NotFound extends ApiError {
        public NotFound(String type, String resource) {
            super(HttpStatus.NOT_FOUND, MessageFormat.format("object {0} of type {1} not found", resource, type));
        }

        @Override
        public boolean shouldLog() {
            return false;
        }

        @Override
        public String getUserMessage() {
            return this.getMessage();
        }
    }

    public static class ServerError extends ApiError {
        public ServerError(String message) {
            super(HttpStatus.INTERNAL_SERVER_ERROR, message);
        }
    }

    public static class AuthenticationError extends ApiError {
        public AuthenticationError(String message) {
            super(HttpStatus.UNAUTHORIZED, "Authentication failed: " + message);
        }

        @Override
        public boolean shouldLog() {
            return false;
        }
    }
}
