package com.openrecordsmanager.plugin.dto;

import com.openrecordsmanager.plugin.PersistedPlugin;
import com.openrecordsmanager.plugin.PluginManager;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record PluginResponse(
        @NotBlank String id,
        @NotBlank String version,
        @NotNull boolean enabled,
        @NotNull Instant dateCreated,
        @NotNull Instant dateModified,
        @NotNull boolean loaded
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
}
