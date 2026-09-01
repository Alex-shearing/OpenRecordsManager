package com.openrecordsmanager.plugin.dto;

import com.openrecordsmanager.plugin.PersistedPlugin;
import com.openrecordsmanager.plugin.PluginManager;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record SimplePluginResponse(
        @NotBlank String name,
        @NotBlank String version,
        boolean enabled,
        @NotBlank Instant dateModified,
        boolean loaded
) {
    public static SimplePluginResponse of(PersistedPlugin plugin, PluginManager pluginManager) {
        return new SimplePluginResponse(
                plugin.getName(),
                plugin.getVersion(),
                plugin.isEnabled(),
                plugin.getDateModified(),
                pluginManager.isLoaded(plugin.getName())
        );
    }

    public static SimplePluginResponse ofLocal(
            String name,
            String version,
            Instant dateModified,
            PluginManager pluginManager
    ) {
        return new SimplePluginResponse(
                name,
                version,
                true,
                dateModified,
                pluginManager.isLoaded(name)
        );
    }
}
