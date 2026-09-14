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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Stream;

@Service
public class PluginManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginManager.class);
    private static final LoadedPlugin BUILTIN_PLUGIN = new LoadedPlugin(
            new LocalPluginInfo(BuiltinPlugin.BUILTIN_PLUGIN_NAME, "latest", "Builtin Components", "These are the builtin components, they cannot be modified.", null),
            new BuiltinPlugin()
    );

    private final Path directory;
    private final PluginRepository pluginRepo;

    private ImmutableList<LoadedPlugin> loadedPlugins = ImmutableList.of(BUILTIN_PLUGIN);
    private @Nullable URLClassLoader classLoader;

    public PluginManager(
            @Value("${server.plugins.directory}") String pluginDirectory,
            PluginRepository pluginRepo
    ) {
        this.directory = Path.of(pluginDirectory);
        this.pluginRepo = pluginRepo;
        this.reload(null);
    }

    public synchronized void reload(@Nullable Set<String> enabledPluginIds) {
        LocalPluginInfo[] localPlugins = this.loadLocalPluginFiles();
        LocalPluginInfo[] pluginsToLoad;
        if (enabledPluginIds == null) {
            pluginsToLoad = localPlugins;
        } else {
            pluginsToLoad = Arrays.stream(localPlugins)
                    .filter(plugin -> enabledPluginIds.contains(plugin.id()))
                    .toArray(LocalPluginInfo[]::new);
        }
        this.instantiatePlugins(pluginsToLoad);
    }

    public List<LoadedPlugin> getLoadedPlugins() {
        return this.loadedPlugins;
    }

    public Optional<LoadedPlugin> getLoadedPlugin(String id) {
        return this.loadedPlugins.stream().filter(plugin -> plugin.id().equals(id)).findFirst();
    }

    public boolean isLoaded(String name) {
        return this.getLoadedPlugin(name).isPresent();
    }

    public Path getDirectory() {
        return this.directory;
    }

    LocalPluginInfo[] loadLocalPluginFiles() {
        if (!Files.exists(this.directory) || !Files.isDirectory(this.directory)) {
            LOGGER.warn("Plugin directory '{}' not found, plugins will not be loaded", this.directory);
            return new LocalPluginInfo[0];
        }

        try (Stream<Path> files = Files.list(this.directory)) {
            return files.map(path -> {
                        try {
                            return LocalPluginInfo.read(path);
                        } catch (IOException | IllegalArgumentException e) {
                            LOGGER.error("Failed to load {} for {}", LocalPluginInfo.FILE_NAME, path.getFileName(), e);
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .toArray(LocalPluginInfo[]::new);
        } catch (IOException e) {
            LOGGER.warn("Error accessing plugin directory '{}', plugins will not be loaded", this.directory, e);
            return new LocalPluginInfo[0];
        }
    }

    void uploadPlugin(
            ComponentCatalog catalog,
            FileStore fileStore,
            PersistedPlugin persistedPlugin,
            LocalPluginInfo localPlugin
    ) throws IOException {
        persistedPlugin.setVersion(localPlugin.version());
        persistedPlugin.setFile(fileStore.newFile(
                catalog,
                Files.newInputStream(localPlugin.getPathOrThrow()),
                localPlugin.extension()
        ));
        LOGGER.info("{} has been uploaded from {}", localPlugin.id(), localPlugin.getPathOrThrow());
        this.pluginRepo.saveAndFlush(persistedPlugin);
    }

    void downloadPlugin(ComponentCatalog catalog, FileStore fileStore, PersistedPlugin persistedPlugin) throws IOException {
        if (persistedPlugin.getFile() == null) {
            LOGGER.warn("attempted to download a plugin {} that does not have an associated file store entry", persistedPlugin.getName());
            return;
        }

        Path destFile = this.getDestFile(persistedPlugin);
        LOGGER.info("Downloading {} to {}", persistedPlugin.getName(), destFile);
        Files.createDirectories(this.directory);

        try (InputStream inputStream = fileStore.getFile(catalog, persistedPlugin.getFile())) {
            Files.copy(inputStream, destFile, StandardCopyOption.REPLACE_EXISTING);
        }

        LOGGER.info("{} has been downloaded to {}", persistedPlugin.getName(), destFile);
    }

    private Path getDestFile(PersistedPlugin persistedPlugin) {
        if (persistedPlugin.getFile() == null) {
            throw new IllegalStateException(
                    "Cannot download plugin " + persistedPlugin.getName() + " without a file store entry"
            );
        }

        String extension = persistedPlugin.getFile().extension;
        if (extension == null || extension.isBlank()) {
            extension = "jar";
        }
        return this.directory.resolve(
                String.format("%s-%s.%s", persistedPlugin.getName(), persistedPlugin.getVersion(), extension)
        );
    }

    void deleteLocalPlugin(PersistedPlugin plugin) throws IOException {
        LocalPluginInfo[] localPlugins = this.loadLocalPluginFiles();
        for (LocalPluginInfo localPlugin : localPlugins) {
            if (Objects.equals(localPlugin.id(), plugin.getName())) {
                Files.deleteIfExists(localPlugin.getPathOrThrow());
                return;
            }
        }
    }

    Set<String> getEnabledPluginNames() {
        return this.pluginRepo.findByEnabledTrue().stream()
                .map(PersistedPlugin::getName)
                .collect(java.util.stream.Collectors.toSet());
    }

    private void instantiatePlugins(LocalPluginInfo[] pluginList) {
        List<LoadedPlugin> loadedPlugins = new ArrayList<>();
        loadedPlugins.add(BUILTIN_PLUGIN);

        // Load jar plugins
        LocalPluginInfo[] jars = Arrays.stream(pluginList)
                .filter(info -> info.extension().equals("jar"))
                .toArray(LocalPluginInfo[]::new);

        URL[] urls = new URL[jars.length];
        for (int i = 0; i < jars.length; i++) {
            try {
                urls[i] = jars[i].getPathOrThrow().toAbsolutePath().normalize().toUri().toURL();
            } catch (MalformedURLException e) {
                LOGGER.error("Failed to load URL for plugin stream {}", jars[i].getFileName());
            }
        }

        this.close();
        this.classLoader = new URLClassLoader(urls, this.getClass().getClassLoader());

        for (Plugin plugin : ServiceLoader.load(Plugin.class, this.classLoader)) {
            try {
                URI jarUrl = plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().normalize();
                LocalPluginInfo info = Arrays.stream(jars)
                        .filter(i -> i.path() != null && i.path().toUri().normalize().equals(jarUrl))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Failed to find plugins local data " + jarUrl));
                loadedPlugins.add(new LoadedPlugin(info, plugin));
            } catch (Exception e) {
                LOGGER.error("Failed to load plugin jar for {}", plugin.getClass().getName(), e);
            }
        }

        // Load zip plugins
        LocalPluginInfo[] zips = Arrays.stream(pluginList)
                .filter(info -> info.extension().equals("zip"))
                .toArray(LocalPluginInfo[]::new);

        for (LocalPluginInfo zip : zips) {
            loadedPlugins.add(new LoadedPlugin(zip, new JsonTemplatePackPlugin()));
        }

        this.loadedPlugins = ImmutableList.copyOf(loadedPlugins);
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
