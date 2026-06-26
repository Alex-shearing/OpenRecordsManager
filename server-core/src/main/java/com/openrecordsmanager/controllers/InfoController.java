package com.openrecordsmanager.controllers;

import com.openrecordsmanager.api.Plugin;
import com.openrecordsmanager.api.config.ConfigStore;
import com.openrecordsmanager.config.ConfigProperties;
import com.openrecordsmanager.resources.PluginManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/info")
public class InfoController {

    private final PluginManager pluginManager;
    private final ConfigStore config;

    public InfoController(PluginManager pluginManager, ConfigStore config) {
        this.pluginManager = pluginManager;
        this.config = config;
    }

    @GetMapping
    public ApiResponse<EnvironmentResponse> getEnvironment() {
        return ApiResponse.success(new EnvironmentResponse(
                this.config.getProperty(ConfigProperties.WORKGROUP_NAME),
                this.pluginManager.getPlugins().stream().map(Plugin::getName).toArray(String[]::new),
                this.config.getProperty(ConfigProperties.WORKGROUP_DATABASE_URL)
        ));
    }

    public record EnvironmentResponse(String workgroup, String[] plugins, String database) {
    }

}
