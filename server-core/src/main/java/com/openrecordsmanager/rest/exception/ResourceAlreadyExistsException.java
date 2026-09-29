package com.openrecordsmanager.rest.exception;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.types.ComponentType;

public class ResourceAlreadyExistsException extends ResourceInUseException {
    public ResourceAlreadyExistsException(String type, String resource) {
        super("already_exists", type, resource);
    }

    public ResourceAlreadyExistsException(ComponentType<?> type, ResourceIdentifier resource) {
        this(type.toString(), resource.toString());
    }
}
