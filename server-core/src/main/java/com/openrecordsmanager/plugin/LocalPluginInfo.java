package com.openrecordsmanager.plugin;

import com.openrecordsmanager.api.schema.JsonSchemaValidator;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Plugin identity and display metadata from {@code plugin.json}, optionally bound to an on-disk
 * jar/zip when loaded from the plugins' directory.
 */
public record LocalPluginInfo(
        String id,
        String version,
        String displayName,
        String description,
        @Nullable Path path
) {
    public static final String FILE_NAME = "plugin.json";

    public static LocalPluginInfo read(Path pluginFile) throws IOException {
        try (ZipFile archive = new ZipFile(pluginFile.toFile())) {
            return read(archive, pluginFile);
        }
    }

    private static LocalPluginInfo read(ZipFile archive, @Nullable Path pluginFile) throws IOException {
        ZipEntry entry = archive.getEntry(FILE_NAME);
        if (entry == null) {
            throw new IOException("missing " + FILE_NAME);
        }
        try (InputStream in = archive.getInputStream(entry)) {
            return parseWithDescriptorFile(in, pluginFile);
        }
    }

    public static LocalPluginInfo readFromStream(InputStream zipStream) throws IOException {
        LocalPluginInfo info = null;
        try (ZipInputStream zis = new ZipInputStream(zipStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(FILE_NAME)) {
                    // this closes the entry stream
                    info = parseWithDescriptorFile(zis, null);
                    break;
                }
                zis.closeEntry();
            }
        }

        if (info == null) {
            throw new IOException("missing " + FILE_NAME);
        }

        return info;
    }

    public static LocalPluginInfo parseWithDescriptorFile(InputStream descriptorFile, @Nullable Path file) throws IOException {
        JsonNode node = JsonSchemaValidator.MAPPER.readTree(descriptorFile);
        if (node == null || !node.isObject()) {
            throw new IOException(FILE_NAME + " must be a JSON object");
        }

        String id = requireNonBlank(node, "id");
        String version = requireNonBlank(node, "version");
        String displayName = requireNonBlank(node, "displayName");
        String description = requireNonBlank(node, "description");

        return new LocalPluginInfo(id, version, displayName, description, file);
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

    public File getFileOrThrow() {
        if (this.path == null) {
            throw new IllegalArgumentException("tried to access file of a runtime plugin");
        }
        return this.path.toFile();
    }

    public Path getPathOrThrow() {
        return this.getFileOrThrow().toPath();
    }

    public String extension() {
        String fileName = this.getFileName();
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "jar";
        }
        return fileName.substring(dot + 1);
    }
}
