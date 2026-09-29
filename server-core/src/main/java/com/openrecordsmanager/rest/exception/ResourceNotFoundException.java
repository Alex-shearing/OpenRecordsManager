package com.openrecordsmanager.rest.exception;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.errors.ApiException;
import com.openrecordsmanager.api.types.ComponentType;

import java.util.UUID;

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String type, String resource) {
        super("resource_not_found", resource, type);
    }

    public ResourceNotFoundException(String type, UUID resource) {
        this(type, resource.toString());
    }

    public ResourceNotFoundException(ComponentType<?> type, ResourceIdentifier resource) {
        this(type.toString(), resource.toString());
    }

    public ResourceNotFoundException(ComponentType<?> type, String resource) {
        this(type.toString(), resource);
    }

    public ResourceNotFoundException(ComponentType<?> type, Class<? extends Component> resource) {
        this(type.toString(), resource.getName());
    }
}
