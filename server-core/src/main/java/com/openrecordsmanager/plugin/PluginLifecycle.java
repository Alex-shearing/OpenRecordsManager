package com.openrecordsmanager.plugin;

import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.database.schema.SchemaMigrationReadyEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PluginLifecycle {
    private final PluginSyncService pluginSyncService;

    public PluginLifecycle(PluginSyncService pluginSyncService) {
        this.pluginSyncService = pluginSyncService;
    }

    @EventListener({ApplicationReadyEvent.class, SchemaMigrationReadyEvent.class})
    public void syncWhenSchemaReady() {
        this.pluginSyncService.syncAndReloadOnStartup();
    }

    @Scheduled(fixedDelayString = "${" + BuiltinConfigs.PLUGINS_SYNC_INTERVAL_MS_KEY + ":30000}", initialDelayString = "${" + BuiltinConfigs.PLUGINS_SYNC_INTERVAL_MS_KEY + ":30000}")
    public void pollForChanges() {
        this.pluginSyncService.syncAndReloadIfChanged();
    }
}
