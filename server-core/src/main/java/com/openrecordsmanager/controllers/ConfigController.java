package com.openrecordsmanager.controllers;

import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.config.DynamicConfigService;
import com.openrecordsmanager.controllers.repsonse.InternalServerErrorApiResponse;
import com.openrecordsmanager.model.SystemConfiguration;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ComponentCatalog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/config")
@InternalServerErrorApiResponse
@ApiResponse(responseCode = "200")
public class ConfigController {

    private final DataRepository repository;
    private final DynamicConfigService config;
    private final ComponentCatalog catalog;

    public ConfigController(DataRepository repository, DynamicConfigService config, ComponentCatalog catalog) {
        this.repository = repository;
        this.config = config;
        this.catalog = catalog;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get config values from the database")
    public Map<String, Optional<?>> getConfig() {
        return this.catalog.getComponents(ComponentTypes.CONFIG).stream()
                .map(config -> Map.entry(config.key(), this.repository.configRepo.findByConfigKey(config.key())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @GetMapping(value = "/this_server", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get the config values for this specific server")
    public Map<String, ?> getThisServerEnvironment() {
        return this.catalog.getComponents(ComponentTypes.CONFIG).stream()
                .map(config -> {
                    Object value = this.config.getValue(config);
                    if (value == null) return null;
                    return Map.entry(config.key(), value);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Set a config value in the database")
    public SystemConfiguration setConfig(@PathVariable("id") String id, @RequestBody String value) {
        SystemConfiguration config = this.repository.configRepo.findByConfigKey(id).orElseGet(() -> new SystemConfiguration(id, value));
        config.configValue = value;
        return this.repository.configRepo.saveAndFlush(config);
    }

}
