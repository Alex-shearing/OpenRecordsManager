package com.openrecordsmanager.controllers;

import com.openrecordsmanager.api.filestore.FileStoreType;
import com.openrecordsmanager.controllers.repsonse.InternalServerErrorApiResponse;
import com.openrecordsmanager.controllers.repsonse.NotFoundApiResponse;
import com.openrecordsmanager.controllers.repsonse.errors.ApiError;
import com.openrecordsmanager.model.FileStore;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ComponentCatalog;
import com.openrecordsmanager.resources.ResourceIdentifier;
import com.openrecordsmanager.resources.types.ComponentTypes;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/file_stores")
@InternalServerErrorApiResponse
@ApiResponse(responseCode = "200")
public class FileStoreController {

    private final DataRepository repository;
    private final ComponentCatalog catalog;

    public FileStoreController(DataRepository repository, ComponentCatalog catalog) {
        this.repository = repository;
        this.catalog = catalog;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public UUID[] list() {
        return this.repository.fileStoreRepo.findAllIds();
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @NotFoundApiResponse
    public FileStore<?> get(@PathVariable("id") UUID id) {
        return this.repository.fileStoreRepo.findById(id)
                .orElseThrow(() -> ApiError.notFound("file store", id.toString()));
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @NotFoundApiResponse
    public FileStore<?> newFileStore(@RequestBody NewFileStore input) {
        FileStoreType<?> type = this.catalog.getComponent(ComponentTypes.FILE_STORE_TYPE, input.type)
                .orElseThrow(() -> ApiError.notFound(ComponentTypes.FILE_STORE_TYPE, input.type));

        return this.repository.fileStoreRepo.saveAndFlush(new FileStore<>(type, input.properties));
    }

    public record NewFileStore(ResourceIdentifier type, Map<String, Object> properties) {
    }

    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @NotFoundApiResponse
    public FileStore<?> updateFileStore(@PathVariable("id") UUID id, @RequestBody Map<String, Object> properties) {
        FileStore<?> store = this.repository.fileStoreRepo.findById(id)
                .orElseThrow(() -> ApiError.notFound("file store", id.toString()));

        store.setProperties(properties);

        return this.repository.fileStoreRepo.saveAndFlush(store);
    }

}
