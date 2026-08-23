package com.openrecordsmanager.filestore.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.filestore.store.FileStore;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.rest.errors.ResourceNotFoundException;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record SimpleFileStoreResponse(
        @NotBlank UUID id,
        @NotBlank ResourceIdentifier type) {

    public static SimpleFileStoreResponse of(ComponentCatalog catalog, FileStore fileStore) {
        ResourceIdentifier middlewareType = catalog.getRegistry(ComponentTypes.FILE_STORE)
                .getId(fileStore.getStoreType(catalog))
                .orElseThrow(() -> new ResourceNotFoundException("file store type for", fileStore.getId()));

        return new SimpleFileStoreResponse(fileStore.getId(), middlewareType);
    }
}
