package com.openrecordsmanager.controllers;

import com.openrecordsmanager.api.Plugin;
import com.openrecordsmanager.config.ConfigProperties;
import com.openrecordsmanager.resources.PluginManager;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/info")
public class InfoController {

    private final PluginManager pluginManager;
    private final Environment config;

    public InfoController(PluginManager pluginManager, Environment config) {
        this.pluginManager = pluginManager;
        this.config = config;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public EnvironmentResponse getEnvironment() {
        return new EnvironmentResponse(
                this.config.getProperty(ConfigProperties.WORKGROUP_NAME.id()),
                this.pluginManager.getPlugins().stream().map(Plugin::getName).toArray(String[]::new),
                this.config.getProperty(ConfigProperties.WORKGROUP_DATABASE_URL.id())
        );
    }

    public record EnvironmentResponse(String workgroup, String[] plugins, String database) {
    }

}
