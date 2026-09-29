package com.openrecordsmanager.rest.exception;

import com.openrecordsmanager.api.errors.ApiException;
import com.openrecordsmanager.api.types.ComponentType;

public class ResourceInUseException extends ApiException {

    public ResourceInUseException(String type) {
        super("in_use", type);
    }

    public ResourceInUseException(ComponentType<?> type) {
        this(type.toString());
    }

    protected ResourceInUseException(String code, String... args) {
        super(code, args);
    }
}
