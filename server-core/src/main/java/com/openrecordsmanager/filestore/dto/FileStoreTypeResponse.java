package com.openrecordsmanager.filestore.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.filestore.FileStoreType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.rest.dto.InputFormSchema;
import com.openrecordsmanager.rest.errors.ResourceNotFoundException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FileStoreTypeResponse(
        @NotBlank ResourceIdentifier id,
        @NotNull InputFormSchema settingsSchema
) {
    public static FileStoreTypeResponse of(ComponentCatalog catalog, FileStoreType<?> type) {
        ResourceIdentifier id = catalog.getRegistry(ComponentTypes.FILE_STORE).getId(type)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.FILE_STORE, type.getClass()));

        return new FileStoreTypeResponse(
                id,
                InputFormSchema.from(type.getSettingsClass())
        );
    }
}
