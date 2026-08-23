package com.openrecordsmanager.config;

import com.openrecordsmanager.config.dto.ConfigResponse;
import com.openrecordsmanager.rest.swagger.DefaultApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/config")
@DefaultApiResponses
@PreAuthorize("isAuthenticated()")
public class ConfigController {

    private final ConfigService config;

    public ConfigController(ConfigService config) {
        this.config = config;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get config values from the database")
    public Map<String, Optional<?>> database_retrieve() {
        return this.config.getAllConfig();
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get the value of the config from the database")
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
    public Map<String, ?> server_retrieveAll() {
        return this.config.getServerConfig();
    }

    @GetMapping(value = "/this_server/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get the effective value for the config this specific server")
    public Object server_retrieveOne(@PathVariable("id") String id) {
        return this.config.getServerConfig(id);
    }
}
