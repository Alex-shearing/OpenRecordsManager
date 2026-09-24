package com.openrecordsmanager.plugin;

import com.google.common.io.MoreFiles;
import com.openrecordsmanager.api.schema.JsonSchemaValidator;
import com.openrecordsmanager.filestore.store.FileStore;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import org.jspecify.annotations.Nullable;
import org.semver4j.Semver;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Plugin identity and display metadata from {@code plugin.json}, optionally bound to an on-disk
 * jar/zip when loaded from the plugins' directory.
 */
public record DiscoveredPlugin(
        String id,
        Semver version,
        String displayName,
        String description,
        @Nullable Path path,
        @Nullable PersistedPlugin persistedPlugin
) {
    public static final String FILE_NAME = "plugin.json";

    public static DiscoveredPlugin read(Path pluginFile, @Nullable PluginRepository repository) throws IOException {
        try (ZipFile archive = new ZipFile(pluginFile.toFile())) {
            return read(archive, pluginFile, repository);
        }
    }

    private static DiscoveredPlugin read(ZipFile archive, @Nullable Path pluginFile, @Nullable PluginRepository repo) throws IOException {
        ZipEntry entry = archive.getEntry(FILE_NAME);
        if (entry == null) {
            throw new IOException("missing " + FILE_NAME);
        }
        try (InputStream in = archive.getInputStream(entry)) {
            return parseWithDescriptorFile(in, pluginFile, repo);
        }
    }

    public static DiscoveredPlugin parseWithDescriptorFile(InputStream descriptorFile, @Nullable Path file, @Nullable PluginRepository repo) throws IOException {
        JsonNode node = JsonSchemaValidator.MAPPER.readTree(descriptorFile);
        if (node == null || !node.isObject()) {
            throw new IOException(FILE_NAME + " must be a JSON object");
        }

        String id = requireNonBlank(node, "id");
        String version = requireNonBlank(node, "version");
        String displayName = requireNonBlank(node, "displayName");
        String description = requireNonBlank(node, "description");

        return new DiscoveredPlugin(
                id,
                new Semver(version),
                displayName,
                description,
                file,
                repo != null ? repo.findById(id).orElse(null) : null
        );
    }

    private static String requireNonBlank(JsonNode node, String field) {
        String value = node.optional(field).map(JsonNode::asString).orElse("");
        if (value.isBlank()) {
            throw new IllegalArgumentException(FILE_NAME + " requires a non-blank " + field);
        }
        return value;
    }

    public String getFileName() {
        if (this.path == null) {
            return "<builtin>";
        }
        return this.path.getFileName().toString();
    }

    public Path getPathOrThrow() {
        if (this.path == null) {
            throw new IllegalArgumentException("tried to access file of a runtime plugin");
        }
        return this.path;
    }

    public boolean isZip() {
        return MoreFiles.getFileExtension(this.getPathOrThrow()).equalsIgnoreCase("zip");
    }

    public boolean isJar() {
        return MoreFiles.getFileExtension(this.getPathOrThrow()).equalsIgnoreCase("jar");
    }

    public boolean isEnabled() {
        return this.persistedPlugin == null || this.persistedPlugin.isEnabled();
    }

    public boolean synchronizeWithServer(PluginManager pluginManager, ComponentCatalog catalog, @Nullable FileStore fileStore) throws IOException {
        if (!this.isEnabled()) {
            PluginSyncService.LOGGER.info("Skipping sync for disabled plugin {}", this.id());
            return false;
        }

        return switch (this.compareLocalToPersisted()) {
            case LOCAL_ONLY -> {
                PluginSyncService.LOGGER.info(
                        "This server has a new plugin {} that does not exist in the database, it will be uploaded",
                        this.id
                );

                PersistedPlugin newPlugin = new PersistedPlugin(this.id, this.version.toString());
                pluginManager.uploadPlugin(catalog, fileStore, newPlugin, this);

                yield false;
            }
            case SAME_VERSION_NO_PERSISTED_FILE -> {
                Objects.requireNonNull(this.persistedPlugin);

                PluginSyncService.LOGGER.info(
                        "Plugin {} is registered locally without a file store entry, uploading",
                        this.id
                );
                pluginManager.uploadPlugin(catalog, fileStore, this.persistedPlugin, this);
                yield false;
            }
            case LOCAL_NEWER -> {
                Objects.requireNonNull(this.persistedPlugin);

                PluginSyncService.LOGGER.info(
                        "This server has a newer version of the {} plugin than the database ({} > {}), it will be uploaded",
                        this.id,
                        this.version,
                        this.persistedPlugin.getVersion()
                );

                pluginManager.uploadPlugin(catalog, fileStore, this.persistedPlugin, this);
                yield false;
            }
            case PERSISTED_NEWER -> {
                Objects.requireNonNull(this.persistedPlugin);

                PluginSyncService.LOGGER.info(
                        "There is a newer version of the {} plugin in the database ({} > {}), it will be downloaded",
                        this.persistedPlugin.getName(),
                        this.persistedPlugin.getVersion(),
                        this.version()
                );

                Files.deleteIfExists(this.getPathOrThrow());
                pluginManager.downloadPlugin(catalog, this.persistedPlugin);

                yield true;
            }
            case SAME_VERSION_HASH_MISMATCH -> {
                Objects.requireNonNull(this.persistedPlugin);

                PluginSyncService.LOGGER.warn(
                        "This server and the database both have {} version {}, but with a different hash. The local version will be reuploaded",
                        this.persistedPlugin.getName(),
                        this.persistedPlugin.getVersion()
                );

                pluginManager.uploadPlugin(catalog, fileStore, this.persistedPlugin, this);

                yield false;
            }
            case EQUAL -> {
                Objects.requireNonNull(this.persistedPlugin);

                PluginSyncService.LOGGER.info(
                        "This server already has the same version of the {} plugin as the database {}",
                        this.persistedPlugin.getName(),
                        this.persistedPlugin.getVersion()
                );

                yield false;
            }
        };
    }

    public PluginComparison compareLocalToPersisted() {
        if (this.persistedPlugin == null) {
            return PluginComparison.LOCAL_ONLY;
        }
        Semver persistedVersion = new Semver(this.persistedPlugin().getVersion());

        if (this.version.isGreaterThan(persistedVersion)) {
            return PluginComparison.LOCAL_NEWER;
        } else if (this.version.isLowerThan(persistedVersion)) {
            return PluginComparison.PERSISTED_NEWER;
        }

        if (this.persistedPlugin.getFile() == null) {
            return PluginComparison.SAME_VERSION_NO_PERSISTED_FILE;
        }

        if (!this.persistedPlugin.getFile().fileMatches(this.getPathOrThrow())) {
            return PluginComparison.SAME_VERSION_HASH_MISMATCH;
        }

        return PluginComparison.EQUAL;
    }

    public enum PluginComparison {
        LOCAL_ONLY,
        LOCAL_NEWER,
        PERSISTED_NEWER,
        SAME_VERSION_HASH_MISMATCH,
        SAME_VERSION_NO_PERSISTED_FILE,
        EQUAL,
    }
}
