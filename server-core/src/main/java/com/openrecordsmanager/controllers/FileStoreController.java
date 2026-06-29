package com.openrecordsmanager.controllers;

import com.openrecordsmanager.api.filestore.FileStoreType;
import com.openrecordsmanager.controllers.errors.ApiError;
import com.openrecordsmanager.model.FileStore;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ComponentCatalog;
import com.openrecordsmanager.resources.ResourceIdentifier;
import com.openrecordsmanager.resources.types.ComponentTypes;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/file_stores")
public class FileStoreController {

    private final DataRepository repository;
    private final ComponentCatalog catalog;

    public FileStoreController(DataRepository repository, ComponentCatalog catalog) {
        this.repository = repository;
        this.catalog = catalog;
    }

    @GetMapping
    public ApiResponse<UUID[]> list() {
        return ApiResponse.success(this.repository.fileStoreRepo.findAllIds());
    }

    @GetMapping("/{id}")
    public ApiResponse<FileStore<?>> get(@PathVariable("id") UUID id) {
        FileStore<?> store = this.repository.fileStoreRepo.findById(id)
                .orElseThrow(() -> ApiError.notFound("file store", id.toString()));

        return ApiResponse.success(store);
    }

    @PostMapping
    public ApiResponse<FileStore<?>> newFileStore(@RequestBody NewFileStore input) {
        FileStoreType<?> type = this.catalog.getComponent(ComponentTypes.FILE_STORE_TYPE, input.type)
                .orElseThrow(() -> ApiError.notFound(ComponentTypes.FILE_STORE_TYPE, input.type));

        return ApiResponse.success(this.repository.fileStoreRepo.saveAndFlush(new FileStore<>(type, input.properties)));
    }

    public record NewFileStore(ResourceIdentifier type, Map<String, Object> properties) {
    }

    @PutMapping("/{id}")
    public ApiResponse<FileStore<?>> updateFileStore(@PathVariable("id") UUID id, @RequestBody Map<String, Object> properties) {
        FileStore<?> store = this.repository.fileStoreRepo.findById(id)
                .orElseThrow(() -> ApiError.notFound("file store", id.toString()));

        store.setProperties(properties);

        return ApiResponse.success(this.repository.fileStoreRepo.saveAndFlush(store));
    }

}
