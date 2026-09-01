package com.openrecordsmanager.plugin.dto;

import com.openrecordsmanager.plugin.PersistedPlugin;
import com.openrecordsmanager.plugin.PluginManager;
import org.jspecify.annotations.NonNull;

import java.time.Instant;

public record SimplePluginResponse(
        @NonNull String name,
        @NonNull String version,
        boolean enabled,
        @NonNull Instant dateModified,
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
