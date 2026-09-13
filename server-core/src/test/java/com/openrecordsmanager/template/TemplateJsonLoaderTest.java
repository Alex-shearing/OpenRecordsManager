package com.openrecordsmanager.template;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.RegistrationContext;
import com.openrecordsmanager.api.template.list.ListElementTemplate;
import com.openrecordsmanager.api.template.list.ListTemplate;
import com.openrecordsmanager.api.template.property.ObjectPropertyTemplate;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class TemplateJsonLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void registersJsonFilesUsingFolderAndFilename() throws Exception {
        Path root = tempDir.resolve("plugin-a");
        writeTemplates(root);

        RecordingContext context = new RecordingContext();
        TemplateJsonLoader.registerFromPath(context, root);

        assertEquals(List.of("colors", "red", "favorite_color"), context.ids());
        assertInstanceOf(ListTemplate.class, context.components().get("colors"));
        assertInstanceOf(ListElementTemplate.class, context.components().get("red"));
        assertInstanceOf(ObjectPropertyTemplate.class, context.components().get("favorite_color"));
        assertEquals("Colors", ((ListTemplate) context.components().get("colors")).name());
        assertEquals(1, ((ListElementTemplate) context.components().get("red")).index());
    }

    @Test
    void ignoresMissingFolders() throws Exception {
        Path root = tempDir.resolve("empty-plugin");
        Files.createDirectories(root);

        RecordingContext context = new RecordingContext();
        TemplateJsonLoader.registerFromPath(context, root);
        assertTrue(context.ids().isEmpty());
    }

    @Test
    void onlyRegistersTemplatesFromPluginOwnJar() throws Exception {
        Path templatesRoot = tempDir.resolve("templates-src");
        writeTemplates(templatesRoot);

        Path ausGovJar = tempDir.resolve("aus-gov.jar");
        Path otherJar = tempDir.resolve("other.jar");
        buildJar(ausGovJar, templatesRoot);
        buildJar(otherJar, null);

        RecordingContext ausGovContext = new RecordingContext();
        RecordingContext otherContext = new RecordingContext();
        TemplateJsonLoader.registerFromPath(ausGovContext, ausGovJar);
        TemplateJsonLoader.registerFromPath(otherContext, otherJar);

        assertFalse(ausGovContext.ids().isEmpty());
        assertTrue(otherContext.ids().isEmpty(), "other plugin must not inherit aus-gov templates");
    }

    @Test
    void registersFromZipArchive() throws Exception {
        Path templatesRoot = tempDir.resolve("zip-templates");
        writeTemplates(templatesRoot);
        Path zip = tempDir.resolve("pack.zip");
        buildJar(zip, templatesRoot);

        RecordingContext context = new RecordingContext();
        TemplateJsonLoader.registerFromPath(context, zip);

        assertEquals(List.of("colors", "red", "favorite_color"), context.ids());
    }

    private void writeTemplates(Path root) throws Exception {
        Path listDir = root.resolve("list");
        Path elementDir = root.resolve("list_element");
        Path propertyDir = root.resolve("object_property");
        Files.createDirectories(listDir);
        Files.createDirectories(elementDir);
        Files.createDirectories(propertyDir);

        Files.writeString(listDir.resolve("colors.json"), """
                {
                  "name": "Colors",
                  "defaultEntries": {}
                }
                """);
        Files.writeString(elementDir.resolve("red.json"), """
                {
                  "name": "Red",
                  "index": 1,
                  "parent": "list/test:colors"
                }
                """);
        Files.writeString(propertyDir.resolve("favorite_color.json"), """
                {
                  "name": "Favorite Color",
                  "type": "list_item",
                  "description": "A color",
                  "listType": "list/test:colors"
                }
                """);
    }

    private void buildJar(Path jar, Path templatesRoot) throws Exception {
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jar))) {
            if (templatesRoot == null) {
                jos.putNextEntry(new JarEntry("META-INF/"));
                jos.closeEntry();
                return;
            }
            try (var paths = Files.walk(templatesRoot)) {
                for (Path path : paths.filter(Files::isRegularFile).toList()) {
                    String entryName = templatesRoot.relativize(path).toString().replace('\\', '/');
                    jos.putNextEntry(new JarEntry(entryName));
                    jos.write(Files.readAllBytes(path));
                    jos.closeEntry();
                }
            }
        }
    }

    private static final class RecordingContext implements RegistrationContext {
        private final Map<String, Component> registered = new LinkedHashMap<>();

        @Override
        public String getName() {
            return "test";
        }

        @Override
        public void registerComponent(@NonNull String id, @NonNull Component component) {
            this.registered.put(id, component);
        }

        List<String> ids() {
            return new ArrayList<>(this.registered.keySet());
        }

        Map<String, Component> components() {
            return this.registered;
        }
    }
}
