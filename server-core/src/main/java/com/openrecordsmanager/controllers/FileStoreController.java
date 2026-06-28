package com.openrecordsmanager.controllers;

import com.openrecordsmanager.model.FileStore;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ResourceIdentifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/file_stores")
public class FileStoreController {

    private final DataRepository repository;

    public FileStoreController(DataRepository repository) {
        this.repository = repository;
    }

    @GetMapping()
    public ApiResponse<String> get() {
        return ApiResponse.success("ok");
    }

    @PostMapping()
    public FileStore newFileStore() {
        FileStore store = new FileStore(ResourceIdentifier.valueOf("filestore_s3:s3"), Map.of());
        return this.repository.fileStoreRepo.saveAndFlush(store);
    }

}
