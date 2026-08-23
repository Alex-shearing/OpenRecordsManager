package com.openrecordsmanager.filestore.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.filestore.middleware.Middleware;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.rest.errors.ResourceNotFoundException;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record SimpleMiddlewareResponse(
        @NotBlank UUID id,
        @NotBlank ResourceIdentifier type) {

    public static SimpleMiddlewareResponse of(ComponentCatalog catalog, Middleware middleware) {
        ResourceIdentifier middlewareType = catalog.getRegistry(ComponentTypes.FILE_STORE_MIDDLEWARE)
                .getId(middleware.getMiddlewareType(catalog))
                .orElseThrow(() -> new ResourceNotFoundException("middleware type for", middleware.getId()));

        return new SimpleMiddlewareResponse(middleware.getId(), middlewareType);
    }
}
