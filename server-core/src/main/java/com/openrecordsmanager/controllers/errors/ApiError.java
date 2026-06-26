package com.openrecordsmanager.controllers.errors;

import com.openrecordsmanager.resources.ResourceIdentifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.text.MessageFormat;

public class ApiError extends RuntimeException {
    public HttpStatusCode httpStatusCode;

    public ApiError(HttpStatusCode code, String message) {
        super(message);
        this.httpStatusCode = code;
    }

    public static ApiError notFound(String type, String resource) {
        throw new NotFound(type, resource);
    }

    public static ApiError notFound(String type, ResourceIdentifier resource) {
        throw new NotFound(type, resource.toString());
    }

    public static ApiError serverError(String pattern, Object... params) {
        throw new ServerError(MessageFormat.format(pattern, params));
    }

    public static ApiError authError(String error) {
        throw new AuthenticationError(error);
    }

    public boolean shouldLog() {
        return true;
    }

    public String getUserMessage() {
        return "Internal Server Error";
    }

    public static class NotFound extends ApiError {
        public NotFound(String type, String resource) {
            super(HttpStatus.NOT_FOUND, MessageFormat.format("{0} resource {1} not found", type, resource));
        }

        @Override
        public boolean shouldLog() {
            return false;
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
