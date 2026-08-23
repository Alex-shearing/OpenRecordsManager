package com.openrecordsmanager.filestore;

import com.openrecordsmanager.filestore.dto.*;
import com.openrecordsmanager.filestore.middleware.MiddlewareService;
import com.openrecordsmanager.filestore.store.FileStoreService;
import com.openrecordsmanager.rest.swagger.ConflictApiResponse;
import com.openrecordsmanager.rest.swagger.DefaultApiResponses;
import com.openrecordsmanager.rest.swagger.NotFoundApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/file_stores")
@DefaultApiResponses
@PreAuthorize("isAuthenticated()")
public class FileStoreController {

    private final FileStoreService storeService;
    private final MiddlewareService middlewareService;

    public FileStoreController(FileStoreService storeService, MiddlewareService middlewareService) {
        this.storeService = storeService;
        this.middlewareService = middlewareService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all file stores")
    public Set<SimpleFileStoreResponse> fileStore_retrieveAll() {
        return this.storeService.getAll();
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get file store details")
    @NotFoundApiResponse
    public FileStoreResponse fileStore_retrieveOne(@PathVariable("id") UUID id) {
        return this.storeService.get(id);
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new file store")
    @NotFoundApiResponse
    public SimpleFileStoreResponse fileStore_create(@RequestBody NewFileStore input) {
        return this.storeService.create(input);
    }

    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modify file store config")
    @NotFoundApiResponse
    public SimpleFileStoreResponse fileStore_update(@PathVariable("id") UUID id, @RequestBody Map<String, ?> properties) {
        return this.storeService.update(id, properties);
    }

    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete a file store")
    @NotFoundApiResponse
    @ConflictApiResponse
    public void fileStore_delete(@PathVariable("id") UUID id) {
        this.storeService.delete(id);
    }

    @GetMapping(value = "/middlewares", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all file store middlewares")
    public Set<SimpleMiddlewareResponse> middleware_retrieveAll() {
        return this.middlewareService.getAll();
    }

    @GetMapping(value = "/middlewares/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get file store middleware details")
    @NotFoundApiResponse
    public MiddlewareResponse middleware_retrieveOne(@PathVariable("id") UUID id) {
        return this.middlewareService.get(id);
    }

    @PostMapping(value = "/middlewares", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new file store middleware")
    @NotFoundApiResponse
    public SimpleMiddlewareResponse middleware_create(@RequestBody NewFileStoreMiddleware input) {
        return this.middlewareService.create(input);
    }

    @PutMapping(value = "/middlewares/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modify file store middleware config")
    @NotFoundApiResponse
    public SimpleMiddlewareResponse middleware_update(@PathVariable("id") UUID id, @RequestBody Map<String, ?> properties) {
        return this.middlewareService.update(id, properties);
    }

    @DeleteMapping(value = "/middlewares/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete a file store middleware")
    @NotFoundApiResponse
    @ConflictApiResponse
    public void middleware_delete(@PathVariable("id") UUID id) {
        this.middlewareService.delete(id);
    }

    @GetMapping(value = "/types", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all supported file store types")
    public FileStoreTypeResponse[] fileStoreType_get() {
        return this.storeService.getTypes();
    }

    @GetMapping(value = "/middlewares/types", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all supported file store middleware types")
    public MiddlewareTypeResponse[] fileStoreMiddlewareType_get() {
        return this.middlewareService.getTypes();
    }

}
