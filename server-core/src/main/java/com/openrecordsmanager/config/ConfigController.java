package com.openrecordsmanager.config;

import com.openrecordsmanager.api.config.ConfigType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.config.dto.ConfigResponse;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.rest.errors.ResourceNotFoundException;
import com.openrecordsmanager.rest.swagger.DefaultApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/config")
@DefaultApiResponses
@PreAuthorize("isAuthenticated()")
public class ConfigController {

    private final ConfigService config;
    private final ComponentCatalog catalog;

    public ConfigController(ConfigService config, ComponentCatalog catalog) {
        this.config = config;
        this.catalog = catalog;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get config values from the database")
    @Transactional(readOnly = true)
    public Map<String, Optional<?>> database_retrieve() {
        return this.config.getAllConfig();
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get the value of the config from the database")
    @Transactional(readOnly = true)
    public Optional<?> database_retrieveOne(@PathVariable("id") String id) {
        return this.config.getDatabaseConfig(id);
    }

    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Set a config value in the database")
    public ConfigResponse set(@PathVariable("id") String id, @RequestBody String value) {
        return this.config.setConfig(id, value);
    }

    @GetMapping(value = "/this_server", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get the effective config values for this specific server")
    @Transactional(readOnly = true)
    public Map<String, ?> server_retrieveAll() {
        return this.catalog.getRegistry(ComponentTypes.CONFIG).stream()
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
        ConfigType<?> config = this.config.getConfigByKey(id)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.CONFIG.name, id));

        return this.config.getOptional(config)
                .orElseThrow(() -> new ResourceNotFoundException("config value", id));
    }

}
