package com.openrecordsmanager.plugin;

import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.config.ConfigService;
import com.openrecordsmanager.database.DatabaseWritableProbe;
import com.openrecordsmanager.filestore.store.FileStore;
import com.openrecordsmanager.filestore.store.FileStoreRepository;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class PluginSyncService {
    public static final Logger LOGGER = LoggerFactory.getLogger(PluginSyncService.class);

    private final PluginManager pluginManager;
    private final ComponentCatalog componentCatalog;
    private final PluginRepository pluginRepository;
    private final FileStoreRepository fileStoreRepository;
    private final ConfigService configService;
    private final DatabaseWritableProbe databaseWritableProbe;

    private Instant lastSeenMaxDateModified = Instant.EPOCH;

    public PluginSyncService(
            PluginManager pluginManager,
            ComponentCatalog componentCatalog,
            PluginRepository pluginRepository,
            FileStoreRepository fileStoreRepository,
            ConfigService configService,
            DatabaseWritableProbe databaseWritableProbe
    ) {
        this.pluginManager = pluginManager;
        this.componentCatalog = componentCatalog;
        this.pluginRepository = pluginRepository;
        this.fileStoreRepository = fileStoreRepository;
        this.configService = configService;
        this.databaseWritableProbe = databaseWritableProbe;
    }

    public void syncAndReloadOnStartup() {
        if (this.isSyncSkipped()) {
            LOGGER.info("Plugin sync is disabled, skipping startup re-sync");
            return;
        }

        LOGGER.info("Conducting startup plugin re-sync");
        this.syncAndReload(false);
    }

    public void syncAndReloadIfChanged() {
        if (this.isSyncSkipped() || !this.databaseWritableProbe.isWritable()) {
            return;
        }

        Optional<Instant> maxModified = this.pluginRepository.findMaxDateModified();
        if (maxModified.isEmpty() || !maxModified.get().isAfter(this.lastSeenMaxDateModified)) {
            return;
        }

        LOGGER.info("Remote plugin changes detected, conducting plugin sync");
        this.syncAndReload(false);
    }

    public synchronized void syncAndReload(boolean force) {
        if (!force && this.isSyncSkipped()) {
            return;
        }

        boolean changed = this.synchronizeWithServer();
        if (changed || force) {
            this.pluginRepository.flush();
            this.pluginManager.reload(this.componentCatalog);
        }
        this.refreshLastSeenMaxDateModified();
    }

    private boolean synchronizeWithServer() {
        LOGGER.info("Synchronizing local plugins with database");

        FileStore fileStore = this.configService.getOptional(BuiltinConfigs.DEFAULT_FILE_STORE)
                .flatMap(this.fileStoreRepository::findById)
                .orElse(null);

        List<PersistedPlugin> missingPlugins = new ArrayList<>(this.pluginRepository.findAll());
        boolean needsReload = false;

        for (DiscoveredPlugin localPlugin : this.pluginManager.discoverLocalPlugins()) {
            missingPlugins.removeIf(persistedPlugin -> Objects.equals(persistedPlugin.getName(), localPlugin.id()));

            try {
                // Sync the discovered plugin with the server, it will return true if we need to reload
                if (localPlugin.synchronizeWithServer(this.pluginManager, this.componentCatalog, fileStore)) {
                    needsReload = true;
                }
            } catch (IOException e) {
                LOGGER.error("Failed to synchronize plugin {} with server", localPlugin.id(), e);
            }
        }

        for (PersistedPlugin plugin : missingPlugins) {
            if (!plugin.isEnabled()) {
                continue;
            }

            if (plugin.getFile() == null) {
                LOGGER.warn(
                        "Plugin {} is in the database but has no file store entry and is not present locally",
                        plugin.getName()
                );
                continue;
            }

            LOGGER.info("There is a new plugin {} available, it will be downloaded", plugin.getName());
            try {
                this.pluginManager.downloadPlugin(this.componentCatalog, plugin);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            needsReload = true;
        }

        return needsReload;
    }

    private void refreshLastSeenMaxDateModified() {
        this.lastSeenMaxDateModified = this.pluginRepository.findMaxDateModified().orElse(Instant.EPOCH);
    }

    private boolean isSyncSkipped() {
        return this.configService.getOrThrow(BuiltinConfigs.PLUGINS_SKIP_SYNC);
    }
}
