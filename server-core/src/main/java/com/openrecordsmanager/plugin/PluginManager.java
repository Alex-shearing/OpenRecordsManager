package com.openrecordsmanager.plugin;

import com.google.common.collect.ImmutableList;
import com.google.common.hash.Hashing;
import com.openrecordsmanager.api.Plugin;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.builtin.BuiltinPlugin;
import com.openrecordsmanager.api.errors.ApiError;
import com.openrecordsmanager.config.DynamicConfigService;
import com.openrecordsmanager.filestore.FileStore;
import com.openrecordsmanager.filestore.FileStoreRepository;
import jakarta.annotation.PreDestroy;
import org.jspecify.annotations.Nullable;
import org.semver4j.Semver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

@Service
public class PluginManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginManager.class);

    private final Path directory;
    private final PluginRepository pluginRepo;
    private final FileStoreRepository fileStoreRepo;

    private ImmutableList<Plugin> plugins;
    private @Nullable URLClassLoader classLoader;

    public PluginManager(
            DynamicConfigService config,
            PluginRepository pluginRepo,
            FileStoreRepository fileStoreRepo
    ) {
        this.directory = Path.of(config.getOrThrow(BuiltinConfigs.PLUGINS_DIRECTORY));
        this.pluginRepo = pluginRepo;
        this.fileStoreRepo = fileStoreRepo;

        this.instantiatePlugins(this.getLocalPlugins());
    }

    private LocalPluginInfo[] getLocalPlugins() {
        File loc = this.directory.toFile();
        if (!loc.exists() || !loc.isDirectory()) {
            LOGGER.warn("Plugin directory '{}' not found, plugins will not be loaded", this.directory);
            return new LocalPluginInfo[0];
        }

        LOGGER.info("Loading plugins from '{}'.", loc.getAbsolutePath());

        File[] files = loc.listFiles((_, name) -> name.endsWith(".jar"));
        if (files == null) {
            LOGGER.warn("Failed to get files from plugin directory, plugins will not be loaded");
            return new LocalPluginInfo[0];
        }

        return Arrays.stream(files)
                .map(this::getPluginInfo)
                .filter(Objects::nonNull)
                .peek(info ->
                        LOGGER.info("Loading {} plugin version {} from {}", info.name, info.version, info.file.getPath()))
                .toArray(LocalPluginInfo[]::new);
    }

    private void instantiatePlugins(LocalPluginInfo[] jarList) {
        URL[] urls = new URL[jarList.length];
        for (int i = 0; i < jarList.length; i++) {
            try {
                urls[i] = jarList[i].file.toURI().toURL();
            } catch (MalformedURLException e) {
                LOGGER.error("Failed to load URL for plugin file {}", jarList[i].file.getName());
            }
        }

        this.close();
        // Create an isolated ClassLoader so plugins don't corrupt Server Core classpath
        this.classLoader = new URLClassLoader(urls, this.getClass().getClassLoader());

        // Use ServiceLoader to discover implementations inside the JARs
        ServiceLoader<Plugin> loader = ServiceLoader.load(Plugin.class, this.classLoader);

        List<Plugin> loadedPlugins = new ArrayList<>();
        loadedPlugins.add(new BuiltinPlugin());

        // Initialize all the plugins
        for (Plugin plugin : loader) {
            loadedPlugins.add(plugin);
        }

        this.plugins = ImmutableList.copyOf(loadedPlugins);
    }

    public List<Plugin> getPlugins() {
        return plugins;
    }

    @PreDestroy
    public void close() {
        if (this.classLoader != null) {
            try {
                LOGGER.info("Closing plugin ClassLoader...");
                this.classLoader.close();
            } catch (IOException e) {
                LOGGER.error("Failed to close plugin ClassLoader", e);
            }
        }
    }

    @Nullable
    private LocalPluginInfo getPluginInfo(File pluginFile) {
        try (JarFile jar = new JarFile(pluginFile)) {
            Manifest manifest = jar.getManifest();
            Attributes attributes = manifest.getMainAttributes();
            return new LocalPluginInfo(
                    attributes.getValue("Plugin-Id"),
                    new Semver(attributes.getValue("Plugin-Version")),
                    pluginFile
            );
        } catch (IOException e) {
            LOGGER.error("Failed to load plugin manifest for {}", pluginFile.getName(), e);
        }
        return null;
    }

    /**
     * Synchronizes the local plugin store with the database plugin store
     *
     * @param catalog      the component catalog
     * @param defaultStore the default store for new plugin files
     * @return true if a new plugin was loaded
     */
    public boolean synchronizeWithServer(ComponentCatalog catalog, UUID defaultStore) {
        LOGGER.info("Starting post component catalog load re-check");

        LocalPluginInfo[] localPluginInfos = this.getLocalPlugins();

        FileStore<?> fileStore = this.fileStoreRepo.findById(defaultStore)
                .orElseThrow(() -> ApiError.notFound("file store", defaultStore.toString()));

        List<PersistedPlugin> missingPlugins = this.pluginRepo.findAll();

        boolean needsReload = false;

        for (LocalPluginInfo localPlugin : localPluginInfos) {
            Optional<PersistedPlugin> optPersistedPlugin = this.pluginRepo.findById(localPlugin.name);

            // New plugin to upload to database
            if (optPersistedPlugin.isEmpty()) {
                LOGGER.info("This server has a new plugin {} that does not exist in the database, it will be uploaded", localPlugin.name);
                PersistedPlugin newPlugin = new PersistedPlugin(localPlugin.name, localPlugin.version.toString());
                this.uploadPlugin(catalog, fileStore, newPlugin, localPlugin);
                continue;
            }

            // Plugin is present on the local server, do not download it
            missingPlugins.removeIf(plugin -> Objects.equals(plugin.name, localPlugin.name));

            // Get the persisted plugin and compare to the local
            PersistedPlugin persistedPlugin = optPersistedPlugin.get();
            Semver persistedVersion = new Semver(persistedPlugin.version);

            if (localPlugin.version.isGreaterThan(persistedVersion)) {
                LOGGER.info("This server has a newer version of the {} plugin than the database ({} > {}), it will be uploaded", localPlugin.name, localPlugin.version, optPersistedPlugin.get().version);
                this.uploadPlugin(catalog, fileStore, persistedPlugin, localPlugin);
                continue;
            }

            if (localPlugin.version.isLowerThan(persistedVersion)) {
                LOGGER.info("There is a newer version of the {} plugin in the database ({} > {}), it will be downloaded", persistedPlugin.name, persistedPlugin.version, localPlugin.version);

                // Remove the old file
                this.close();
                try {
                    Files.deleteIfExists(localPlugin.file.toPath());
                } catch (IOException e) {
                    LOGGER.error("Failed to delete old plugin file {}", localPlugin.file.getPath(), e);
                }

                // Download new file
                this.downloadPlugin(catalog, fileStore, persistedPlugin);

                // A new plugin was downloaded, so mark for reload
                needsReload = true;
                continue;
            }

            try {
                String localHash = com.google.common.io.Files.asByteSource(localPlugin.file)
                        .hash(Hashing.sha256())
                        .toString();

                if (!localHash.equals(persistedPlugin.file.hash)) {
                    LOGGER.warn(
                            "This server and the database both have {} version {}, but with a different hash ({} != {}). The local version will be reuploaded",
                            persistedPlugin.name, persistedPlugin.version,
                            localHash, persistedPlugin.file.hash
                    );

                    this.uploadPlugin(catalog, fileStore, persistedPlugin, localPlugin);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            LOGGER.info("This server already has the same version of the {} plugin as the database {}", persistedPlugin.name, persistedPlugin.version);
        }

        // Download any new plugins that don't exist locally
        for (PersistedPlugin plugin : missingPlugins) {
            LOGGER.info("There is a new plugin {} available, it will be downloaded", plugin.name);
            this.downloadPlugin(catalog, fileStore, plugin);

            // A new plugin was downloaded, so mark for reload
            needsReload = true;
        }

        // If a new/updated plugin was downloaded, reload the plugins
        if (needsReload) {
            this.instantiatePlugins(this.getLocalPlugins());
            return true;
        }

        return false;
    }

    private void uploadPlugin(ComponentCatalog catalog, FileStore<?> fileStore, PersistedPlugin persistedPlugin, LocalPluginInfo localPlugin) {
        try {
            persistedPlugin.version = localPlugin.version.toString();
            persistedPlugin.file = fileStore.newFile(catalog, new FileInputStream(localPlugin.file), "jar");
            LOGGER.info("{} has been uploaded from {}", localPlugin.name, localPlugin.file.getPath());
        } catch (IOException e) {
            LOGGER.error("Failed to upload file {}", localPlugin.file.getPath(), e);
        }
        this.pluginRepo.save(persistedPlugin);
    }

    private void downloadPlugin(ComponentCatalog catalog, FileStore<?> fileStore, PersistedPlugin persistedPlugin) {
        Path destFile = this.directory.resolve(String.format("%s-%s.jar", persistedPlugin.name, persistedPlugin.version));
        LOGGER.info("Downloading {} to {}", persistedPlugin.name, destFile);

        try {
            InputStream inputStream = fileStore.getFile(catalog, persistedPlugin.file);
            Files.copy(
                    inputStream,
                    destFile,
                    StandardCopyOption.REPLACE_EXISTING
            );

            LOGGER.info("{} has been downloaded to {}", persistedPlugin.name, destFile);
        } catch (IOException e) {
            LOGGER.error("Failed to download plugin {} to file {}", persistedPlugin.name, destFile, e);
        }
    }

    private record LocalPluginInfo(String name, Semver version, File file) {
    }
}
