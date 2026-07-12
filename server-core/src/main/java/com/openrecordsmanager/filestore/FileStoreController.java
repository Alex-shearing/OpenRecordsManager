package com.openrecordsmanager.filestore;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.errors.ApiError;
import com.openrecordsmanager.api.filestore.FileStoreMiddlewareType;
import com.openrecordsmanager.api.filestore.FileStoreType;
import com.openrecordsmanager.api.swagger.DefaultErrorResponses;
import com.openrecordsmanager.api.swagger.NotFoundApiResponse;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.plugin.ComponentCatalog;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/file_stores")
@DefaultErrorResponses
@PreAuthorize("isAuthenticated()")
public class FileStoreController {

    private final DataRepository repository;
    private final ComponentCatalog catalog;

    public FileStoreController(DataRepository repository, ComponentCatalog catalog) {
        this.repository = repository;
        this.catalog = catalog;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all file stores")
    public UUID[] fileStore_retrieveAll() {
        return this.repository.fileStoreRepo.findAllIds();
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get file store details")
    @NotFoundApiResponse
    public FileStore<?> fileStore_retrieveOne(@PathVariable("id") UUID id) {
        return this.repository.fileStoreRepo.findById(id)
                .orElseThrow(() -> ApiError.notFound("file store", id.toString()));
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new file store")
    @NotFoundApiResponse
    public FileStore<?> fileStore_create(@RequestBody NewFileStore input) {
        FileStoreType<?> type = this.catalog.getComponent(ComponentTypes.FILE_STORE_TYPE, input.type)
                .orElseThrow(() -> ApiError.notFound(ComponentTypes.FILE_STORE_TYPE, input.type));

        FileStore<?> store = new FileStore<>(this.catalog, type, input.properties);

        for (UUID middleware : input.middlewares) {
            FileStoreMiddleware<?> mw = this.repository.fileStoreMiddlewareRepo.findById(middleware)
                    .orElseThrow(() -> ApiError.notFound("file store middleware", middleware.toString()));

            store.addMiddleware(mw);
        }

        return this.repository.fileStoreRepo.saveAndFlush(store);
    }

    public record NewFileStore(ResourceIdentifier type, Map<String, ?> properties, List<UUID> middlewares) {
    }

    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modify file store config")
    @NotFoundApiResponse
    public FileStore<?> fileStore_update(@PathVariable("id") UUID id, @RequestBody Map<String, ?> properties) {
        FileStore<?> store = this.repository.fileStoreRepo.findById(id)
                .orElseThrow(() -> ApiError.notFound("file store", id.toString()));

        store.setProperties(properties);

        return this.repository.fileStoreRepo.saveAndFlush(store);
    }

    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete a file store")
    @NotFoundApiResponse
    public void fileStore_delete(@PathVariable("id") UUID id) {
        FileStore<?> store = this.repository.fileStoreRepo.findById(id)
                .orElseThrow(() -> ApiError.notFound("file store", id.toString()));

        this.repository.fileStoreRepo.delete(store);
    }

    @GetMapping(value = "/middlewares", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all file store middlewares")
    public UUID[] middleware_retrieveAll() {
        return this.repository.fileStoreMiddlewareRepo.findAllIds();
    }

    @GetMapping(value = "/middlewares/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get file store middleware details")
    @NotFoundApiResponse
    public FileStoreMiddleware<?> middleware_retrieveOne(@PathVariable("id") UUID id) {
        return this.repository.fileStoreMiddlewareRepo.findById(id)
                .orElseThrow(() -> ApiError.notFound("file store middleware", id.toString()));
    }

    @PostMapping(value = "/middlewares", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new file store middleware")
    @NotFoundApiResponse
    public FileStoreMiddleware<?> middleware_create(@RequestBody NewFileStoreMiddleware input) {
        FileStoreMiddlewareType<?> type = this.catalog.getComponent(ComponentTypes.FILE_STORE_MIDDLEWARE, input.type)
                .orElseThrow(() -> ApiError.notFound(ComponentTypes.FILE_STORE_MIDDLEWARE, input.type));

        FileStoreMiddleware<?> middleware = new FileStoreMiddleware<>(this.catalog, type, input.properties);

        return this.repository.fileStoreMiddlewareRepo.saveAndFlush(middleware);
    }

    public record NewFileStoreMiddleware(ResourceIdentifier type, Map<String, ?> properties) {
    }

    @PutMapping(value = "/middlewares/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modify file store middleware config")
    @NotFoundApiResponse
    public FileStoreMiddleware<?> middleware_update(@PathVariable("id") UUID id, @RequestBody Map<String, ?> properties) {
        FileStoreMiddleware<?> middleware = this.repository.fileStoreMiddlewareRepo.findById(id)
                .orElseThrow(() -> ApiError.notFound("file store middleware", id.toString()));

        middleware.setProperties(properties);

        return this.repository.fileStoreMiddlewareRepo.saveAndFlush(middleware);
    }
}
