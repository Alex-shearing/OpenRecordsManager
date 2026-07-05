package com.openrecordsmanager.controllers;

import com.openrecordsmanager.api.Plugin;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.config.DynamicConfigService;
import com.openrecordsmanager.controllers.repsonse.InternalServerErrorApiResponse;
import com.openrecordsmanager.resources.PluginManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/info")
@InternalServerErrorApiResponse
@ApiResponse(responseCode = "200")
public class InfoController {

    private final PluginManager pluginManager;
    private final DynamicConfigService config;

    public InfoController(PluginManager pluginManager, DynamicConfigService config) {
        this.pluginManager = pluginManager;
        this.config = config;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get basic details about the environment")
    public EnvironmentResponse getEnvironment() {
        return new EnvironmentResponse(
                this.config.getOrThrow(BuiltinConfigs.WORKGROUP_NAME),
                this.pluginManager.getPlugins().stream().map(Plugin::getName).toArray(String[]::new),
                this.config.getOrThrow(BuiltinConfigs.DATABASE_PRIMARY_URL)
        );
    }

    public record EnvironmentResponse(String workgroup, String[] plugins, String database) {
    }

}
