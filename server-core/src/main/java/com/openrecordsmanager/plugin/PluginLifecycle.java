package com.openrecordsmanager.plugin;

import com.openrecordsmanager.database.schema.SchemaMigrationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class PluginLifecycle {
    private final PluginSyncService pluginSyncService;

    public PluginLifecycle(PluginSyncService pluginSyncService) {
        this.pluginSyncService = pluginSyncService;
    }

    @EventListener(SchemaMigrationReadyEvent.class)
    public void onSchemaMigrationReady() {
        this.pluginSyncService.syncAndReloadOnStartup();
    }
}
