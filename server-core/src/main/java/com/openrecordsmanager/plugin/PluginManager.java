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
import java.util.zip.ZipFile;

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

        File[] files = loc.listFiles((_, name) -> name.endsWith(".jar") || name.endsWith(".zip"));
        if (files == null) {
            LOGGER.warn("Failed to get files from plugin directory, plugins will not be loaded");
            return new LocalPluginInfo[0];
        }

        return Arrays.stream(files)
                .filter(file -> !file.getName().startsWith("upload-"))
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
        try (ZipFile jar = new ZipFile(pluginFile)) {
            PluginDescriptor descriptor = PluginDescriptor.read(jar);
            return new LocalPluginInfo(descriptor.id(), descriptor.version(), pluginFile);
        } catch (IOException | IllegalArgumentException e) {
            LOGGER.error("Failed to load {} for {}", PluginDescriptor.FILE_NAME, pluginFile.getName(), e);
        }
        return null;
    }

    @Nullable
    LocalPluginInfo getPluginInfo(InputStream archiveStream, Path destFile) throws IOException {
        Files.createDirectories(destFile.getParent());
        Files.copy(archiveStream, destFile, StandardCopyOption.REPLACE_EXISTING);
        return this.getPluginInfo(destFile.toFile());
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
                Files.newInputStream(localPlugin.file().toPath()),
                localPlugin.extension()
        ));
        LOGGER.info("{} has been uploaded from {}", localPlugin.name(), localPlugin.file().getPath());
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

    private void instantiatePlugins(LocalPluginInfo[] pluginList) {
        List<Plugin> loadedPlugins = new ArrayList<>();
        loadedPlugins.add(new BuiltinPlugin());

        // Load jar plugins
        LocalPluginInfo[] jars = Arrays.stream(pluginList)
                .filter(LocalPluginInfo::isJar)
                .toArray(LocalPluginInfo[]::new);

        URL[] urls = new URL[jars.length];
        for (int i = 0; i < jars.length; i++) {
            try {
                urls[i] = jars[i].file().toURI().toURL();
            } catch (MalformedURLException e) {
                LOGGER.error("Failed to load URL for plugin stream {}", jars[i].file().getName());
            }
        }

        this.close();
        this.classLoader = new URLClassLoader(urls, this.getClass().getClassLoader());

        ServiceLoader<Plugin> loader = ServiceLoader.load(Plugin.class, this.classLoader);

        for (Plugin plugin : loader) {
            loadedPlugins.add(plugin);
        }

        // Load zip plugins
        LocalPluginInfo[] zips = Arrays.stream(pluginList)
                .filter(LocalPluginInfo::isZip)
                .toArray(LocalPluginInfo[]::new);

        for (LocalPluginInfo zip : zips) {
            loadedPlugins.add(new JsonTemplatePackPlugin(zip.name(), zip.file().toPath()));
        }

        this.plugins = ImmutableList.copyOf(loadedPlugins);
        this.loadedPersistedNames = Arrays.stream(pluginList)
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
        boolean isJar() {
            return this.file.getName().endsWith(".jar");
        }

        boolean isZip() {
            return this.file.getName().endsWith(".zip");
        }

        String extension() {
            String fileName = this.file.getName();
            int dot = fileName.lastIndexOf('.');
            if (dot < 0 || dot == fileName.length() - 1) {
                return "jar";
            }
            return fileName.substring(dot + 1);
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
