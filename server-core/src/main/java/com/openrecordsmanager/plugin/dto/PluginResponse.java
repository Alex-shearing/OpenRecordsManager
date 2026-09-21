package com.openrecordsmanager.plugin.dto;

import com.openrecordsmanager.plugin.DiscoveredPlugin;
import com.openrecordsmanager.plugin.LoadedPlugin;
import com.openrecordsmanager.plugin.PersistedPlugin;
import com.openrecordsmanager.plugin.PluginManager;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Optional;

public record PluginResponse(
        @NotBlank String id,
        @NotBlank String displayName,
        @NotBlank String description,
        @NotBlank String version,
        @NotNull boolean enabled,
        @NotNull Instant dateCreated,
        @NotNull Instant dateModified,
        @NotNull boolean loaded
) {
    public static PluginResponse of(PersistedPlugin plugin, PluginManager pluginManager) {
        Optional<DiscoveredPlugin> pluginMeta = pluginManager.getLoadedPlugin(plugin.getName()).map(LoadedPlugin::info);
        return new PluginResponse(
                plugin.getName(),
                pluginMeta.map(DiscoveredPlugin::displayName).orElse(plugin.getName()),
                pluginMeta.map(DiscoveredPlugin::description).orElse(plugin.getName()),
                plugin.getVersion(),
                plugin.isEnabled(),
                plugin.getDateCreated(),
                plugin.getDateModified(),
                pluginManager.isLoaded(plugin.getName())
        );
    }
}
