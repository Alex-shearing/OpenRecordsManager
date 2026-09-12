package com.openrecordsmanager.plugin;

import com.openrecordsmanager.api.schema.JsonSchemaValidator;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Identity descriptor shipped at the root of a plugin {@code .jar} or {@code .zip} as {@code plugin.json}.
 */
public record PluginDescriptor(String id, String version) {
    public static final String FILE_NAME = "plugin.json";

    public static PluginDescriptor read(ZipFile archive) throws IOException {
        ZipEntry entry = archive.getEntry(FILE_NAME);
        if (entry == null) {
            throw new IOException("missing " + FILE_NAME);
        }
        try (InputStream in = archive.getInputStream(entry)) {
            return parse(in);
        }
    }

    public static PluginDescriptor parse(InputStream in) throws IOException {
        JsonNode node = JsonSchemaValidator.MAPPER.readTree(in);
        if (node == null || !node.isObject()) {
            throw new IOException(FILE_NAME + " must be a JSON object");
        }
        String id = node.get("id").asString();
        String version = node.get("version").asString();
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(FILE_NAME + " requires a non-blank id");
        }
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException(FILE_NAME + " requires a non-blank version");
        }

        return new PluginDescriptor(id, version);
    }
}
