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
    public Map<String, Optional<?>> getEnvironment() {
        return this.catalog.getComponents(ComponentTypes.CONFIG).stream()
                .map(config -> Map.entry(config.id(), config.type().fromString(this.environment.getProperty(config.id()))))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @PutMapping("/{id}")
    public SystemConfiguration setConfig(@PathVariable("id") String id, @RequestBody String value) {
        SystemConfiguration config = this.repository.configRepo.findByConfigKey(id).orElseGet(() -> new SystemConfiguration(id, value));
        return this.repository.configRepo.saveAndFlush(config);
    }

}
