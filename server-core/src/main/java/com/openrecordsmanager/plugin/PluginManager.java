package com.openrecordsmanager.plugin;

import com.google.common.collect.ImmutableList;
import com.google.common.io.MoreFiles;
import com.openrecordsmanager.api.Plugin;
import com.openrecordsmanager.api.builtin.BuiltinPlugin;
import com.openrecordsmanager.filestore.store.FileStore;
import com.openrecordsmanager.filestore.store.FileStoreEntry;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import jakarta.annotation.PreDestroy;
import org.jspecify.annotations.Nullable;
import org.semver4j.Semver;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class PluginManager {
    public static final Logger LOGGER = LoggerFactory.getLogger(PluginManager.class);
    private static final LoadedPlugin BUILTIN_PLUGIN = new LoadedPlugin(
            new DiscoveredPlugin(
                    BuiltinPlugin.BUILTIN_PLUGIN_NAME,
                    Semver.ZERO,
                    "Builtin Components",
                    "These are the builtin components, they cannot be modified.",
                    null,
                    null
            ),
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

    public synchronized void reload(@Nullable ComponentCatalog catalog) {
        DiscoveredPlugin[] pluginsToLoad = this.discoverLocalPlugins()
                .stream()
                .filter(DiscoveredPlugin::isEnabled)
                .toArray(DiscoveredPlugin[]::new);

        this.loadPlugins(pluginsToLoad);

        if (catalog != null) {
            catalog.reload(this);
        }
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

    Set<DiscoveredPlugin> discoverLocalPlugins() {
        if (!Files.exists(this.directory) || !Files.isDirectory(this.directory)) {
            LOGGER.warn("Plugin directory '{}' not found, plugins will not be loaded", this.directory);
            return Set.of();
        }

        try (Stream<Path> files = Files.list(this.directory)) {
            return files
                    .filter(path -> {
                        String ext = MoreFiles.getFileExtension(path);
                        return ext.equalsIgnoreCase("jar") || ext.equalsIgnoreCase("zip");
                    })
                    .map(path -> {
                        try {
                            return DiscoveredPlugin.read(path, this.pluginRepo);
                        } catch (IOException | IllegalArgumentException e) {
                            LOGGER.error("Failed to load {} for {}", DiscoveredPlugin.FILE_NAME, path.getFileName(), e);
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            LOGGER.warn("Error accessing plugin directory '{}', plugins will not be loaded", this.directory, e);
            return Set.of();
        }
    }

    void uploadPlugin(
            ComponentCatalog catalog,
            @Nullable FileStore fileStore,
            PersistedPlugin persistedPlugin,
            DiscoveredPlugin localPlugin
    ) throws IOException {
        if (fileStore == null) {
            LOGGER.info("{} would have been uploaded from {}, but no file store was provided", localPlugin.id(), localPlugin.getPathOrThrow());
            return;
        }
        persistedPlugin.setVersion(localPlugin.version().toString());

        Path path = localPlugin.getPathOrThrow();
        persistedPlugin.setFile(fileStore.newFile(
                catalog,
                Files.newInputStream(path),
                MoreFiles.getFileExtension(path)
        ));

        LOGGER.info("{} has been uploaded from {}", localPlugin.id(), localPlugin.getPathOrThrow());
        this.pluginRepo.saveAndFlush(persistedPlugin);
    }

    void downloadPlugin(ComponentCatalog catalog, PersistedPlugin persistedPlugin) throws IOException {
        if (persistedPlugin.getFile() == null) {
            LOGGER.warn("attempted to download a plugin {} that does not have an associated file store entry", persistedPlugin.getName());
            return;
        }

        Path destFile = this.getDestFile(persistedPlugin);
        LOGGER.info("Downloading {} to {}", persistedPlugin.getName(), destFile);
        Files.createDirectories(this.directory);

        try (InputStream inputStream = persistedPlugin.getFile().store.getFile(catalog, persistedPlugin.getFile())) {
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
        Set<DiscoveredPlugin> localPlugins = this.discoverLocalPlugins().stream()
                .filter(p -> Objects.equals(p.id(), plugin.getName()))
                .collect(Collectors.toSet());

        for (DiscoveredPlugin localPlugin : localPlugins) {
            Files.deleteIfExists(localPlugin.getPathOrThrow());
            return;
        }
    }

    private void loadPlugins(DiscoveredPlugin[] pluginList) {
        List<LoadedPlugin> loadedPlugins = new ArrayList<>();
        loadedPlugins.add(BUILTIN_PLUGIN);

        // Load jar plugins
        DiscoveredPlugin[] jars = Arrays.stream(pluginList)
                .filter(DiscoveredPlugin::isJar)
                .toArray(DiscoveredPlugin[]::new);

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
                DiscoveredPlugin info = Arrays.stream(jars)
                        .filter(i -> i.path() != null && i.path().toUri().normalize().equals(jarUrl))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Failed to find plugins local data " + jarUrl));
                loadedPlugins.add(new LoadedPlugin(info, plugin));
            } catch (Exception e) {
                LOGGER.error("Failed to load plugin jar for {}", plugin.getClass().getName(), e);
            }
        }

        // Load zip plugins
        DiscoveredPlugin[] zips = Arrays.stream(pluginList)
                .filter(DiscoveredPlugin::isZip)
                .toArray(DiscoveredPlugin[]::new);

        for (DiscoveredPlugin zip : zips) {
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
