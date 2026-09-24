package com.openrecordsmanager.plugin.dto;

import com.openrecordsmanager.plugin.DiscoveredPlugin;
import com.openrecordsmanager.plugin.LoadedPlugin;
import com.openrecordsmanager.plugin.PersistedPlugin;
import com.openrecordsmanager.plugin.PluginManager;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Optional;

public record SimplePluginResponse(
        @NotBlank String id,
        @NotBlank String displayName,
        @NotNull String description,
        @NotBlank String version,
        @NotNull boolean enabled,
        @NotNull Instant dateModified,
        @NotNull boolean loaded
) {
    public static SimplePluginResponse of(PersistedPlugin plugin, PluginManager pluginManager) {
        Optional<DiscoveredPlugin> pluginMeta = pluginManager.getLoadedPlugin(plugin.getName()).map(LoadedPlugin::info);
        return new SimplePluginResponse(
                plugin.getName(),
                pluginMeta.map(DiscoveredPlugin::displayName).orElse(plugin.getName()),
                pluginMeta.map(DiscoveredPlugin::description).orElse(""),
                plugin.getVersion(),
                plugin.isEnabled(),
                plugin.getDateModified(),
                pluginManager.isLoaded(plugin.getName())
        );
    }

    public static SimplePluginResponse of(DiscoveredPlugin plugin, PluginManager pluginManager) {
        Instant dateModified = Instant.EPOCH;
        if (plugin.persistedPlugin() != null) {
            dateModified = plugin.persistedPlugin().getDateModified();
        } else if (plugin.path() != null) {
            try {
                dateModified = Files.getLastModifiedTime(plugin.path()).toInstant();
            } catch (IOException ignored) {
                // keep EPOCH
            }
        }

        return new SimplePluginResponse(
                plugin.id(),
                plugin.displayName(),
                plugin.description(),
                plugin.version().getVersion(),
                plugin.isEnabled(),
                dateModified,
                pluginManager.isLoaded(plugin.id())
        );
    }
}
