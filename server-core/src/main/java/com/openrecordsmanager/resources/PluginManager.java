package com.openrecordsmanager.resources;

import com.openrecordsmanager.api.BuiltinComponents;
import com.openrecordsmanager.api.Plugin;
import com.openrecordsmanager.controllers.repsonse.errors.ApiError;
import com.openrecordsmanager.model.FileStore;
import com.openrecordsmanager.model.PersistedPlugin;
import com.openrecordsmanager.model.repositories.FileStoreRepository;
import com.openrecordsmanager.model.repositories.PluginRepository;
import jakarta.annotation.Nullable;
import jakarta.annotation.PreDestroy;
import org.semver4j.Semver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.stream.Collectors;

@Service
public class PluginManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginManager.class);

    private final Path directory;
    private final PluginRepository pluginRepo;
    private final FileStoreRepository fileStoreRepo;
    private List<Plugin> plugins;
    private URLClassLoader classLoader;

    @Autowired
    public PluginManager(
            @Value("${server.plugins.directory}") String pluginDirectory,
            PluginRepository pluginRepo,
            FileStoreRepository fileStoreRepo,
            Plugin... additionalPlugins
    ) {
        this.directory = Path.of(pluginDirectory);
        this.pluginRepo = pluginRepo;
        this.fileStoreRepo = fileStoreRepo;

        File[] jarList = this.getJarFiles();
        if (jarList == null) {
            LOGGER.warn("Plugin directory '{}' not found. Plugins will not be loaded.", this.directory);
            this.plugins = List.of();
            this.classLoader = null;
            return;
        }

        this.loadPlugins(jarList);
    }

    private File[] getJarFiles() {
        File loc = this.directory.toFile();
        if (!loc.exists() || !loc.isDirectory()) {
            return null;
        }

        LOGGER.info("Loading plugins from '{}'.", loc.getAbsolutePath());

        return loc.listFiles((_, name) -> name.endsWith(".jar"));
    }

    private void loadPlugins(File[] jarList) {
        URL[] urls = new URL[jarList.length];
        for (int i = 0; i < jarList.length; i++) {
            try {
                urls[i] = jarList[i].toURI().toURL();
            } catch (MalformedURLException e) {
                LOGGER.error("Failed to load URL for plugin file {}", jarList[i].getName());
                continue;
            }
            LOGGER.info("Found plugin JAR: {}", jarList[i].getName());
        }

        // Create an isolated ClassLoader so plugins don't corrupt Server Core classpath
        if (this.classLoader != null) {
            try {
                this.classLoader.close();
            } catch (IOException e) {
                LOGGER.error("Failed to close old ClassLoader");
            }
        }
        this.classLoader = new URLClassLoader(urls, this.getClass().getClassLoader());

        // Use ServiceLoader to discover implementations inside the JARs
        ServiceLoader<Plugin> loader = ServiceLoader.load(Plugin.class, this.classLoader);

        List<Plugin> loadedPlugins = new ArrayList<>();
        loadedPlugins.add(new BuiltinComponents());

        // Initialize all the plugins
        for (Plugin plugin : loader) {
//            LOGGER.info("Found plugin '{}' v{}, loading...", info.name, info.version);
            loadedPlugins.add(plugin);
        }

        this.plugins = Collections.unmodifiableList(loadedPlugins);
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
    private PluginInfo getPluginInfo(File pluginFile) {
        try (JarFile jar = new JarFile(pluginFile)) {
            Manifest manifest = jar.getManifest();
            Attributes attributes = manifest.getMainAttributes();
            return new PluginInfo(
                    attributes.getValue("Plugin-Id"),
                    new Semver(attributes.getValue("Plugin-Version")),
                    pluginFile,
                    PluginStatus.NONE
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
    public boolean checkStatus(ComponentCatalog catalog, UUID defaultStore) {
        LOGGER.info("Starting post component catalog load re-check");

        File[] jarList = this.getJarFiles();
        if (jarList == null) {
            LOGGER.warn("Plugin directory '{}' not found. Plugins will not be re-loaded.", this.directory);
            return false;
        }

        Set<PluginInfo> pluginInfos = Arrays.stream(jarList)
                .map(this::getPluginInfo)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (PersistedPlugin persistedPlugin : pluginRepo.findAll()) {
            Semver persistedVersion = new Semver(persistedPlugin.version);
            Optional<PluginInfo> localPlugin = pluginInfos.stream()
                    .filter(pluginInfo -> Objects.equals(pluginInfo.name, persistedPlugin.name))
                    .findFirst();

            if (localPlugin.isEmpty()) {
                LOGGER.info("Persisted plugin {} is not present on this server, it will be downloaded.", persistedPlugin.name);
                pluginInfos.add(new PluginInfo(persistedPlugin.name, persistedVersion, new File(this.directory.toFile(), persistedPlugin.name + "-" + persistedPlugin.version + ".jar"), PluginStatus.DOWNLOAD_FROM_DATABASE));
                continue;
            }

            if (localPlugin.get().version.isGreaterThan(persistedVersion)) {
                LOGGER.info("This server has a newer version of the {} plugin than the database ({} > {}), it will be uploaded.", persistedPlugin.name, localPlugin.get().version, persistedPlugin.version);
                localPlugin.get().status = PluginStatus.UPLOAD_TO_DATABASE;
            } else if (localPlugin.get().version.isLowerThan(persistedVersion)) {
                LOGGER.info("There is a newer version of the {} plugin in the database ({} > {}), it will be downloaded.", persistedPlugin.name, persistedPlugin.version, localPlugin.get().version);
                localPlugin.get().status = PluginStatus.DOWNLOAD_FROM_DATABASE;
            } else {
                LOGGER.info("This server already has the same version of the {} plugin as the database {}.", persistedPlugin.name, persistedPlugin.version);
                localPlugin.get().status = PluginStatus.LOCAL_MATCH;
            }
        }

        FileStore<?> fileStore = this.fileStoreRepo.findById(defaultStore)
                .orElseThrow(() -> ApiError.notFound("file store", defaultStore.toString()));

        boolean needsRestart = false;

        for (PluginInfo pluginAction : pluginInfos) {
            PersistedPlugin persistedPlugin = this.pluginRepo.findById(pluginAction.name)
                    .orElseGet(() -> new PersistedPlugin(pluginAction.name, pluginAction.version.toString()));

            switch (pluginAction.status) {
                case UPLOAD_TO_DATABASE:
                case NONE:
                    try {
                        persistedPlugin.file = fileStore.newFile(catalog, new FileInputStream(pluginAction.file), "jar");
                        persistedPlugin.version = pluginAction.version.toString();
                        LOGGER.info("{} has been uploaded to the database from {}", persistedPlugin.name, pluginAction.file.getPath());
                    } catch (IOException e) {
                        LOGGER.error("Failed to upload file {} to database", pluginAction.file.getPath(), e);
                    }
                    break;
                case DOWNLOAD_FROM_DATABASE:
                    try {
                        InputStream inputStream = fileStore.getFile(catalog, persistedPlugin.file);
                        Files.copy(
                                inputStream,
                                pluginAction.file.toPath(),
                                StandardCopyOption.REPLACE_EXISTING
                        );

                        LOGGER.info("{} has been downloaded from the database to {}, they catalog will need a reload", persistedPlugin.name, pluginAction.file.getPath());

                        // We are downloading a new plugin so will need to restart
                        needsRestart = true;
                    } catch (IOException e) {
                        LOGGER.error("Failed to download plugin {} from the database to file {}", persistedPlugin.name, pluginAction.file.getPath(), e);
                    }
                    break;
            }

            this.pluginRepo.save(persistedPlugin);
        }

        if (needsRestart) {
            this.loadPlugins(this.getJarFiles());
        }

        return needsRestart;
    }

    private enum PluginStatus {
        NONE,
        LOCAL_MATCH,
        UPLOAD_TO_DATABASE,
        DOWNLOAD_FROM_DATABASE
    }

    private static final class PluginInfo {
        private final String name;
        private final Semver version;
        private final File file;
        private PluginStatus status;

        private PluginInfo(String name, Semver version, File file, PluginStatus status) {
            this.name = name;
            this.version = version;
            this.file = file;
            this.status = status;
        }
    }
}
