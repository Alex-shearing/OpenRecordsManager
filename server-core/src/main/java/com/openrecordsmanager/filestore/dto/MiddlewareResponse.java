package com.openrecordsmanager.filestore.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.filestore.middleware.Middleware;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.rest.errors.ResourceNotFoundException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record MiddlewareResponse(
        @NotBlank UUID id,
        @NotBlank ResourceIdentifier type,
        @NotNull Map<String, ?> properties) {

    public static MiddlewareResponse of(ComponentCatalog catalog, Middleware middleware) {
        ResourceIdentifier middlewareType = catalog.getRegistry(ComponentTypes.FILE_STORE_MIDDLEWARE)
                .getId(middleware.getMiddlewareType(catalog))
                .orElseThrow(() -> new ResourceNotFoundException("middleware type for", middleware.getId()));

        return new MiddlewareResponse(middleware.getId(), middlewareType, middleware.getProperties(catalog));
    }

}
