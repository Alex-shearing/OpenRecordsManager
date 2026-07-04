package com.openrecordsmanager.controllers;

import com.openrecordsmanager.api.config.ConfigDefinition;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.config.DynamicConfigService;
import com.openrecordsmanager.controllers.repsonse.InternalServerErrorApiResponse;
import com.openrecordsmanager.controllers.repsonse.errors.ApiError;
import com.openrecordsmanager.model.SystemConfiguration;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ComponentCatalog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
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
    @Transactional(readOnly = true)
    public Map<String, Optional<?>> database_retrieve() {
        return this.repository.configRepo.findAll().stream()
                .map(config -> {
                    ConfigDefinition<?> type = this.config.getConfigByKey(config.configKey, this.catalog)
                            .orElseThrow(() -> ApiError.notFound(ComponentTypes.CONFIG.name, config.configKey));

                    return Map.entry(config.configKey, type.type().fromString(config.configValue));
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get the value of the config from the database")
    @Transactional(readOnly = true)
    public Object database_retrieveOne(@PathVariable("id") String id) {
        ConfigDefinition<?> config = this.config.getConfigByKey(id, this.catalog)
                .orElseThrow(() -> ApiError.notFound(ComponentTypes.CONFIG.name, id));

        SystemConfiguration systemConfiguration = this.repository.configRepo.findByConfigKey(config.key())
                .orElseThrow(() -> ApiError.notFound("config value", id));

        return config.type().fromString(systemConfiguration.configValue);
    }

    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Set a config value in the database")
    public SystemConfiguration set(@PathVariable("id") String id, @RequestBody String value) {
        this.config.getConfigByKey(id, this.catalog)
                .orElseThrow(() -> ApiError.notFound(ComponentTypes.CONFIG.name, id));

        SystemConfiguration config = this.repository.configRepo.findByConfigKey(id)
                .orElseGet(() -> new SystemConfiguration(id, value));
        config.configValue = value;

        return this.repository.configRepo.saveAndFlush(config);
    }

    @GetMapping(value = "/this_server", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get the effective config values for this specific server")
    @Transactional(readOnly = true)
    public Map<String, ?> server_retrieveAll() {
        return this.catalog.getComponents(ComponentTypes.CONFIG).stream()
                .map(config -> {
                    Object value = this.config.getValue(config);
                    if (value == null) return null;
                    return Map.entry(config.key(), value);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @GetMapping(value = "/this_server/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get the effective value for the config this specific server")
    @Transactional(readOnly = true)
    public Object server_retrieveOne(@PathVariable("id") String id) {
        ConfigDefinition<?> config = this.config.getConfigByKey(id, this.catalog)
                .orElseThrow(() -> ApiError.notFound(ComponentTypes.CONFIG.name, id));

        return this.config.getOptional(config)
                .orElseThrow(() -> ApiError.notFound("config value", id));
    }

}
