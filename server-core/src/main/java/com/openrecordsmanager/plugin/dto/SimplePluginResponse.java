package com.openrecordsmanager.plugin.dto;

import com.openrecordsmanager.plugin.DiscoveredPlugin;
import com.openrecordsmanager.plugin.LoadedPlugin;
import com.openrecordsmanager.plugin.PersistedPlugin;
import com.openrecordsmanager.plugin.PluginManager;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Optional;

public record SimplePluginResponse(
        @NotBlank String id,
        @NotBlank String displayName,
        @NotNull String description,
        @NotBlank String version,
        @NotBlank boolean enabled,
        @Nullable Instant dateModified,
        @NotBlank boolean loaded
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
}
