package com.openrecordsmanager.plugin;

import com.openrecordsmanager.api.Plugin;
import com.openrecordsmanager.rest.swagger.DefaultApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/plugins")
@DefaultApiResponses
@PreAuthorize("isAuthenticated()")
public class PluginController {

    private final PluginManager pluginManager;

    public PluginController(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List currently enabled plugins")
    public List<String> list() {
        return this.pluginManager.getPlugins().stream()
                .map(Plugin::getName)
                .toList();
    }
}
