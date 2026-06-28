package com.openrecordsmanager.controllers;

import com.openrecordsmanager.api.filestore.FileStoreType;
import com.openrecordsmanager.controllers.errors.ApiError;
import com.openrecordsmanager.model.FileStore;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ComponentCatalog;
import com.openrecordsmanager.resources.ResourceIdentifier;
import com.openrecordsmanager.resources.types.ComponentTypes;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

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

    @GetMapping()
    public UUID[] list() {
        return this.repository.fileStoreRepo.findAllIds();
    }

    @GetMapping("/{id}")
    public FileStore<?> get(@PathVariable("id") UUID id) {
        return this.repository.fileStoreRepo.findById(id)
                .orElseThrow(() -> ApiError.notFound("file store", id.toString()));
    }

    @PostMapping()
    public FileStore<?> newFileStore() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode object = mapper.createObjectNode();
        object.put("bucket", "bucket");
        object.put("endpoint", "endpoint");

        FileStoreType<?> type = this.catalog.getComponent(ComponentTypes.FILE_STORE_TYPE, ResourceIdentifier.valueOf("filestore_s3:s3")).orElseThrow();

        FileStore<?> store = new FileStore<>(type, object);
        return this.repository.fileStoreRepo.saveAndFlush(store);
    }

}
