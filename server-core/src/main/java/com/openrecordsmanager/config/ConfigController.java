package com.openrecordsmanager.config;

import com.openrecordsmanager.config.dto.ConfigResponse;
import com.openrecordsmanager.config.dto.ConfigTypeResponse;
import com.openrecordsmanager.rest.swagger.DefaultApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
    public Set<ConfigTypeResponse> getAllConfig() {
        return this.config.getAllConfigTypes();
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get the value of the config from the database")
    public Optional<?> getConfig(@PathVariable("id") String id) {
        return this.config.getDatabaseConfig(id);
    }

    @PutMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Set a config value in the database")
    public Set<ConfigResponse> setConfigs(@RequestBody Map<String, Object> values) {
        return values.entrySet().stream()
                .map(e -> this.config.setConfig(e.getKey(), e.getValue()))
                .collect(Collectors.toSet());
    }

    @GetMapping(value = "/this_server", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get the effective config values for this specific server")
    public Map<String, ?> getAllLocalConfig() {
        return this.config.getServerConfig();
    }

    @GetMapping(value = "/this_server/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get the effective value for the config this specific server")
    public Object getLocalConfig(@PathVariable("id") String id) {
        return this.config.getServerConfig(id);
    }
}
