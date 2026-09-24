package com.openrecordsmanager.filestore.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.filestore.middleware.Middleware;
import com.openrecordsmanager.filestore.store.FileStore;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.rest.errors.ResourceNotFoundException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record FileStoreResponse(
        @NotBlank UUID id,
        @NotBlank ResourceIdentifier type,
        @NotNull Map<String, ?> properties,
        @NotNull List<UUID> middlewares,
        @NotNull long fileCount,
        @NotNull Instant dateCreated,
        @NotNull Instant dateModified) {

    public static FileStoreResponse of(ComponentCatalog catalog, FileStore store, long fileCount) {
        ResourceIdentifier storeTypeId = catalog.getRegistry(ComponentTypes.FILE_STORE)
                .getId(store.getStoreType(catalog))
                .orElseThrow(() -> new ResourceNotFoundException("store type for", store.getId()));

        return new FileStoreResponse(
                store.getId(),
                storeTypeId,
                store.getProperties(catalog),
                store.getMiddlewares().stream()
                        .map(Middleware::getId)
                        .toList(),
                fileCount,
                store.getDateCreated(),
                store.getDateModified()
        );
    }
}
