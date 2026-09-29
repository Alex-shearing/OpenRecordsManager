package com.openrecordsmanager.rest.exception;

import com.openrecordsmanager.api.errors.ApiException;

public class ForbiddenException extends ApiException {
    public ForbiddenException(String type) {
        super("forbidden", type);
    }
}
