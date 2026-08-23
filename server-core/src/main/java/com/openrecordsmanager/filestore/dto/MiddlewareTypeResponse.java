package com.openrecordsmanager.filestore.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.filestore.FileStoreMiddlewareType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.rest.InputFormSchema;
import com.openrecordsmanager.rest.errors.ResourceNotFoundException;
import jakarta.validation.constraints.NotBlank;

public record MiddlewareTypeResponse(
        @NotBlank ResourceIdentifier id,
        @NotBlank InputFormSchema settingsSchema
) {
    public static MiddlewareTypeResponse of(ComponentCatalog catalog, FileStoreMiddlewareType<?> type) {
        ResourceIdentifier id = catalog.getRegistry(ComponentTypes.FILE_STORE_MIDDLEWARE).getId(type)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.FILE_STORE_MIDDLEWARE, type.getClass()));

        return new MiddlewareTypeResponse(
                id,
                InputFormSchema.from(type.getSettingsClass())
        );
    }
}
