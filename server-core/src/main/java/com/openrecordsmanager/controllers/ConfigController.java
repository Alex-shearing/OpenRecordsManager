package com.openrecordsmanager.controllers;

import com.openrecordsmanager.api.config.ConfigStore;
import com.openrecordsmanager.model.SystemConfiguration;
import com.openrecordsmanager.model.repositories.DataRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/config")
public class ConfigController {

    private final DataRepository repository;
    private final ConfigStore config;

    public ConfigController(DataRepository repository, ConfigStore config) {
        this.repository = repository;
        this.config = config;
    }

    @GetMapping
    public Map<String, Object> getConfig() {
        return this.config.getProperties().stream()
                .map(config -> Map.entry(config.id(), this.config.getProperty(config)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @PutMapping("/{id}")
    public SystemConfiguration setConfig(@PathVariable("id") String id, @RequestBody String value) {
        SystemConfiguration config = this.repository.configRepo.findByConfigKey(id).orElseGet(() -> new SystemConfiguration(id, value));
        return this.repository.configRepo.saveAndFlush(config);
    }

}
