package com.openrecordsmanager.controllers;

import com.openrecordsmanager.model.repositories.ConfigRepository;
import com.openrecordsmanager.resources.PluginManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/file_store")
public class FileStoreController {

    private final PluginManager pluginManager;
    private final ConfigRepository configRepository;

    // Spring automatically injects the PluginRuntimeManager Bean here
    public FileStoreController(PluginManager pluginManager, ConfigRepository configRepository) {
        this.pluginManager = pluginManager;
        this.configRepository = configRepository;
    }

    @GetMapping()
    public ResponseEntity<ApiResponse<String>> get() {
        System.out.println(configRepository.findByConfigKey("test"));
        return ResponseEntity.ok(ApiResponse.success("ok"));
    }

}
