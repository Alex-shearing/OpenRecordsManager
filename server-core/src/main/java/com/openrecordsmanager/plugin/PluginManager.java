package com.openrecordsmanager.plugin;

import com.google.common.collect.ImmutableList;
import com.openrecordsmanager.api.Plugin;
import com.openrecordsmanager.api.builtin.BuiltinPlugin;
import com.openrecordsmanager.filestore.store.FileStore;
import com.openrecordsmanager.filestore.store.FileStoreEntry;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import jakarta.annotation.PreDestroy;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
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

    private ImmutableList<Plugin> plugins = ImmutableList.of(new BuiltinPlugin());
    private Set<String> loadedPersistedNames = Set.of();
    private @Nullable URLClassLoader classLoader;

    public PluginManager(
            @Value("${server.plugins.directory}") String pluginDirectory,
            PluginRepository pluginRepo
    ) {
        this.directory = Path.of(pluginDirectory);
        this.pluginRepo = pluginRepo;
        this.reload(null);
    }

    public synchronized void reload(@Nullable Set<String> enabledPluginNames) {
        LocalPluginInfo[] localPlugins = this.getLocalPlugins();
        LocalPluginInfo[] pluginsToLoad;
        if (enabledPluginNames == null) {
            pluginsToLoad = localPlugins;
        } else {
            pluginsToLoad = Arrays.stream(localPlugins)
                    .filter(plugin -> enabledPluginNames.contains(plugin.name()))
                    .toArray(LocalPluginInfo[]::new);
        }
        this.instantiatePlugins(pluginsToLoad);
    }

    public List<Plugin> getPlugins() {
        return this.plugins;
    }

    public boolean isLoaded(String name) {
        return this.loadedPersistedNames.contains(name);
    }

    public Path getDirectory() {
        return this.directory;
    }

    LocalPluginInfo[] getLocalPlugins() {
        File loc = this.directory.toFile();
        if (!loc.exists() || !loc.isDirectory()) {
            LOGGER.warn("Plugin directory '{}' not found, plugins will not be loaded", this.directory);
            return new LocalPluginInfo[0];
        }

        File[] files = loc.listFiles((_, name) -> name.endsWith(".jar"));
        if (files == null) {
            LOGGER.warn("Failed to get files from plugin directory, plugins will not be loaded");
            return new LocalPluginInfo[0];
        }

        return Arrays.stream(files)
                .map(this::getPluginInfo)
                .filter(Objects::nonNull)
                .toArray(LocalPluginInfo[]::new);
    }

    Optional<LocalPluginInfo> findLocalPlugin(String name) {
        return Arrays.stream(this.getLocalPlugins())
                .filter(plugin -> Objects.equals(plugin.name(), name))
                .findFirst();
    }

    @Nullable
    LocalPluginInfo getPluginInfo(File pluginFile) {
        try (JarFile jar = new JarFile(pluginFile)) {
            Manifest manifest = jar.getManifest();
            Attributes attributes = manifest.getMainAttributes();
            return new LocalPluginInfo(
                    attributes.getValue("Plugin-Id"),
                    attributes.getValue("Plugin-Version"),
                    pluginFile
            );
        } catch (IOException e) {
            LOGGER.error("Failed to load plugin manifest for {}", pluginFile.getName(), e);
        }
        return null;
    }

    @Nullable
    LocalPluginInfo getPluginInfo(InputStream jarStream, Path destFile) throws IOException {
        Files.createDirectories(destFile.getParent());
        Files.copy(jarStream, destFile, StandardCopyOption.REPLACE_EXISTING);
        return this.getPluginInfo(destFile.toFile());
    }

    void uploadPlugin(
            ComponentCatalog catalog,
            FileStore fileStore,
            PersistedPlugin persistedPlugin,
            LocalPluginInfo localPlugin
    ) throws IOException {
        persistedPlugin.setVersion(localPlugin.version());
        persistedPlugin.setFile(fileStore.newFile(catalog, Files.newInputStream(localPlugin.file().toPath()), "jar"));
        LOGGER.info("{} has been uploaded from {}", localPlugin.name(), localPlugin.file().getPath());
        this.pluginRepo.saveAndFlush(persistedPlugin);
    }

    void downloadPlugin(ComponentCatalog catalog, FileStore fileStore, PersistedPlugin persistedPlugin) throws IOException {
        if (persistedPlugin.getFile() == null) {
            throw new IllegalStateException(
                    "Cannot download plugin " + persistedPlugin.getName() + " without a file store entry"
            );
        }

        Path destFile = this.directory.resolve(String.format("%s-%s.jar", persistedPlugin.getName(), persistedPlugin.getVersion()));
        LOGGER.info("Downloading {} to {}", persistedPlugin.getName(), destFile);
        Files.createDirectories(this.directory);

        try (InputStream inputStream = fileStore.getFile(catalog, persistedPlugin.getFile())) {
            Files.copy(inputStream, destFile, StandardCopyOption.REPLACE_EXISTING);
        }

        LOGGER.info("{} has been downloaded to {}", persistedPlugin.getName(), destFile);
    }

    void deleteLocalPlugin(PersistedPlugin plugin) throws IOException {
        LocalPluginInfo[] localPlugins = this.getLocalPlugins();
        for (LocalPluginInfo localPlugin : localPlugins) {
            if (Objects.equals(localPlugin.name(), plugin.getName())) {
                Files.deleteIfExists(localPlugin.file().toPath());
                return;
            }
        }
    }

    Set<String> getEnabledPluginNames() {
        return this.pluginRepo.findByEnabledTrue().stream()
                .map(PersistedPlugin::getName)
                .collect(java.util.stream.Collectors.toSet());
    }

    private void instantiatePlugins(LocalPluginInfo[] jarList) {
        URL[] urls = new URL[jarList.length];
        for (int i = 0; i < jarList.length; i++) {
            try {
                urls[i] = jarList[i].file().toURI().toURL();
            } catch (MalformedURLException e) {
                LOGGER.error("Failed to load URL for plugin stream {}", jarList[i].file().getName());
            }
        }

        this.close();
        this.classLoader = new URLClassLoader(urls, this.getClass().getClassLoader());

        ServiceLoader<Plugin> loader = ServiceLoader.load(Plugin.class, this.classLoader);

        List<Plugin> loadedPlugins = new ArrayList<>();
        loadedPlugins.add(new BuiltinPlugin());
        for (Plugin plugin : loader) {
            loadedPlugins.add(plugin);
        }

        this.plugins = ImmutableList.copyOf(loadedPlugins);
        this.loadedPersistedNames = Arrays.stream(jarList)
                .map(LocalPluginInfo::name)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
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
            this.classLoader = null;
        }
    }

    record LocalPluginInfo(String name, String version, File file) {
    }

    static Map<String, Object> auditMetadata(PersistedPlugin plugin) {
        FileStoreEntry file = plugin.getFile();
        if (file == null) {
            return Map.of("version", plugin.getVersion());
        }
        return Map.of(
                "fileHash", file.hash,
                "hashAlgorithm", file.hashAlgorithm,
                "version", plugin.getVersion()
        );
    }
}
