package com.openrecordsmanager.plugin.dto;

import com.openrecordsmanager.plugin.PersistedPlugin;
import com.openrecordsmanager.plugin.PluginManager;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record PluginResponse(
        @NotBlank String name,
        @NotBlank String version,
        boolean enabled,
        @NotBlank Instant dateCreated,
        @NotBlank Instant dateModified,
        boolean loaded
) {
    public static PluginResponse of(PersistedPlugin plugin, PluginManager pluginManager) {
        return new PluginResponse(
                plugin.getName(),
                plugin.getVersion(),
                plugin.isEnabled(),
                plugin.getDateCreated(),
                plugin.getDateModified(),
                pluginManager.isLoaded(plugin.getName())
        );
    }

    public static PluginResponse ofLocal(
            String name,
            String version,
            Instant dateModified,
            PluginManager pluginManager
    ) {
        return new PluginResponse(
                name,
                version,
                true,
                dateModified,
                dateModified,
                pluginManager.isLoaded(name)
        );
    }
}
