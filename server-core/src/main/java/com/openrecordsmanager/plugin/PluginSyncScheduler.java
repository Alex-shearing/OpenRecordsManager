package com.openrecordsmanager.plugin;

import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PluginSyncScheduler {
    private final PluginSyncService pluginSyncService;

    public PluginSyncScheduler(PluginSyncService pluginSyncService) {
        this.pluginSyncService = pluginSyncService;
    }

    @Scheduled(fixedDelayString = "${" + BuiltinConfigs.PLUGINS_SYNC_INTERVAL_MS_KEY + ":30000}")
    public void pollForChanges() {
        this.pluginSyncService.syncAndReloadIfChanged();
    }
}
