package com.openrecordsmanager.api.errors;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.types.ComponentType;

import java.text.MessageFormat;
import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String type, String resource) {
        super(MessageFormat.format("object {0} of type {1} not found", resource, type));
    }

    public ResourceNotFoundException(String type, UUID resource) {
        super(MessageFormat.format("object {0} of type {1} not found", resource, type));
    }

    public ResourceNotFoundException(ComponentType<?> type, ResourceIdentifier resource) {
        this(type.toString(), resource.toString());
    }
}
