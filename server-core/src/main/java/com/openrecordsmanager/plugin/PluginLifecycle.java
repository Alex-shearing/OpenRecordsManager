package com.openrecordsmanager.plugin;

import com.openrecordsmanager.database.schema.SchemaMigrationReadyEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
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
}
