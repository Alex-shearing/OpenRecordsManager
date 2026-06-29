package com.openrecordsmanager.controllers;

import com.openrecordsmanager.model.SystemConfiguration;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ComponentCatalog;
import com.openrecordsmanager.resources.types.ComponentTypes;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/config")
public class ConfigController {

    private final DataRepository repository;
    private final Environment environment;
    private final ComponentCatalog catalog;

    public ConfigController(DataRepository repository, Environment config, ComponentCatalog catalog) {
        this.repository = repository;
        this.environment = config;
        this.catalog = catalog;
    }

    @GetMapping
    public ApiResponse<Map<String, Optional<?>>> getConfig() {
        Map<String, Optional<?>> properties = this.catalog.getComponents(ComponentTypes.CONFIG).stream()
                .map(config -> Map.entry(config.id(), this.repository.configRepo.findByConfigKey(config.id())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return ApiResponse.success(properties);
    }

    @GetMapping("/this_server")
    public ApiResponse<Map<String, Optional<?>>> getThisServerEnvironment() {
        Map<String, Optional<?>> properties = this.catalog.getComponents(ComponentTypes.CONFIG).stream()
                .map(config -> Map.entry(config.id(), config.type().fromString(this.environment.getProperty(config.id()))))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return ApiResponse.success(properties);
    }

    @PutMapping("/{id}")
    public ApiResponse<SystemConfiguration> setConfig(@PathVariable("id") String id, @RequestBody String value) {
        SystemConfiguration config = this.repository.configRepo.findByConfigKey(id).orElseGet(() -> new SystemConfiguration(id, value));
        config.configValue = value;
        return ApiResponse.success(this.repository.configRepo.saveAndFlush(config));
    }

}
