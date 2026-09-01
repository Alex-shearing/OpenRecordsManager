package com.openrecordsmanager.plugin;

import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.config.ConfigService;
import com.openrecordsmanager.database.DatabaseWritableProbe;
import com.openrecordsmanager.filestore.store.FileStore;
import com.openrecordsmanager.filestore.store.FileStoreRepository;
import com.openrecordsmanager.filestore.store.FileStoreService;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.rest.errors.ResourceNotFoundException;
import org.semver4j.Semver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;

@Service
public class PluginSyncService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginSyncService.class);

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
            LOGGER.info("Plugin sync is disabled, skipping startup sync");
            this.refreshLastSeenMaxDateModified();
            return;
        }
        this.syncAndReload(true);
    }

    public void syncAndReloadIfChanged() {
        if (this.isSyncSkipped() || !this.databaseWritableProbe.isWritable()) {
            return;
        }

        Optional<Instant> maxModified = this.pluginRepository.findMaxDateModified();
        if (maxModified.isEmpty() || !maxModified.get().isAfter(this.lastSeenMaxDateModified)) {
            return;
        }

        this.syncAndReload(false);
    }

    public synchronized void syncAndReload(boolean force) {
        if (!force && this.isSyncSkipped()) {
            return;
        }

        Optional<UUID> defaultStore = this.configService.getOptional(BuiltinConfigs.DEFAULT_FILE_STORE);
        if (defaultStore.isEmpty()) {
            LOGGER.debug("No default file store configured, registering local plugins without file upload");
            this.registerLocalPluginsOnly();
            this.reloadEnabledPlugins();
            this.refreshLastSeenMaxDateModified();
            return;
        }

        boolean changed = this.synchronizeWithServer(defaultStore.get());
        if (changed || force) {
            this.reloadEnabledPlugins();
        }
        this.refreshLastSeenMaxDateModified();
    }

    private boolean synchronizeWithServer(UUID defaultStore) {
        LOGGER.info("Synchronizing local plugins with database");

        PluginManager.LocalPluginInfo[] localPluginInfos = this.pluginManager.getLocalPlugins();
        FileStore fileStore = this.fileStoreRepository.findById(defaultStore)
                .orElseThrow(() -> new ResourceNotFoundException("default store", defaultStore.toString()));

        List<PersistedPlugin> missingPlugins = new ArrayList<>(this.pluginRepository.findAll());
        boolean needsReload = false;

        for (PluginManager.LocalPluginInfo localPlugin : localPluginInfos) {
            Optional<PersistedPlugin> optPersistedPlugin = this.pluginRepository.findById(localPlugin.name());

            if (optPersistedPlugin.isEmpty()) {
                LOGGER.info(
                        "This server has a new plugin {} that does not exist in the database, it will be uploaded",
                        localPlugin.name()
                );
                PersistedPlugin newPlugin = new PersistedPlugin(localPlugin.name(), localPlugin.version());
                try {
                    this.pluginManager.uploadPlugin(this.componentCatalog, fileStore, newPlugin, localPlugin);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                continue;
            }

            missingPlugins.removeIf(plugin -> Objects.equals(plugin.getName(), localPlugin.name()));

            PersistedPlugin persistedPlugin = optPersistedPlugin.get();
            if (!persistedPlugin.isEnabled()) {
                LOGGER.info("Skipping sync for disabled plugin {}", localPlugin.name());
                continue;
            }

            if (persistedPlugin.getFile() == null) {
                LOGGER.info(
                        "Plugin {} is registered locally without a file store entry, uploading",
                        localPlugin.name()
                );
                try {
                    this.pluginManager.uploadPlugin(this.componentCatalog, fileStore, persistedPlugin, localPlugin);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                continue;
            }

            Semver persistedVersion = new Semver(persistedPlugin.getVersion());
            Semver localVersion = new Semver(localPlugin.version());

            if (localVersion.isGreaterThan(persistedVersion)) {
                LOGGER.info(
                        "This server has a newer version of the {} plugin than the database ({} > {}), it will be uploaded",
                        localPlugin.name(),
                        localPlugin.version(),
                        persistedPlugin.getVersion()
                );
                try {
                    this.pluginManager.uploadPlugin(this.componentCatalog, fileStore, persistedPlugin, localPlugin);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                continue;
            }

            if (localVersion.isLowerThan(persistedVersion)) {
                LOGGER.info(
                        "There is a newer version of the {} plugin in the database ({} > {}), it will be downloaded",
                        persistedPlugin.getName(),
                        persistedPlugin.getVersion(),
                        localPlugin.version()
                );

                try {
                    Files.deleteIfExists(localPlugin.file().toPath());
                    this.pluginManager.downloadPlugin(this.componentCatalog, fileStore, persistedPlugin);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                needsReload = true;
                continue;
            }

            try {
                String localHash = com.google.common.io.Files.asByteSource(localPlugin.file())
                        .hash(FileStoreService.getHashFunction(persistedPlugin.getFile().hashAlgorithm))
                        .toString();

                if (!localHash.equals(persistedPlugin.getFile().hash)) {
                    LOGGER.warn(
                            "This server and the database both have {} version {}, but with a different hash ({} != {}). The local version will be reuploaded",
                            persistedPlugin.getName(),
                            persistedPlugin.getVersion(),
                            localHash,
                            persistedPlugin.getFile().hash
                    );
                    this.pluginManager.uploadPlugin(this.componentCatalog, fileStore, persistedPlugin, localPlugin);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            LOGGER.info(
                    "This server already has the same version of the {} plugin as the database {}",
                    persistedPlugin.getName(),
                    persistedPlugin.getVersion()
            );
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
                this.pluginManager.downloadPlugin(this.componentCatalog, fileStore, plugin);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            needsReload = true;
        }

        return needsReload;
    }

    private void registerLocalPluginsOnly() {
        for (PluginManager.LocalPluginInfo localPlugin : this.pluginManager.getLocalPlugins()) {
            Optional<PersistedPlugin> existing = this.pluginRepository.findById(localPlugin.name());
            if (existing.isEmpty()) {
                LOGGER.info("Registering local plugin {} in database", localPlugin.name());
                this.pluginRepository.save(new PersistedPlugin(localPlugin.name(), localPlugin.version()));
                continue;
            }

            PersistedPlugin persistedPlugin = existing.get();
            Semver localVersion = new Semver(localPlugin.version());
            Semver persistedVersion = new Semver(persistedPlugin.getVersion());
            if (localVersion.isGreaterThan(persistedVersion)) {
                LOGGER.info(
                        "Updating local plugin {} from version {} to {}",
                        localPlugin.name(),
                        persistedPlugin.getVersion(),
                        localPlugin.version()
                );
                persistedPlugin.setVersion(localPlugin.version());
                this.pluginRepository.save(persistedPlugin);
            }
        }
    }

    private void reloadEnabledPlugins() {
        this.pluginRepository.flush();
        Set<String> enabledPluginNames = this.pluginManager.getEnabledPluginNames();
        this.pluginManager.reload(enabledPluginNames);
        this.componentCatalog.reload(this.pluginManager);
    }

    private void refreshLastSeenMaxDateModified() {
        this.lastSeenMaxDateModified = this.pluginRepository.findMaxDateModified().orElse(Instant.EPOCH);
    }

    private boolean isSyncSkipped() {
        return this.configService.getOrThrow(BuiltinConfigs.PLUGINS_SKIP_SYNC);
    }
}
