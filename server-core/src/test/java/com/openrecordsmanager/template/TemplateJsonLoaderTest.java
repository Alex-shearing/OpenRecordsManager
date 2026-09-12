package com.openrecordsmanager.template;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.RegistrationContext;
import com.openrecordsmanager.api.template.list.ListElementTemplate;
import com.openrecordsmanager.api.template.list.ListTemplate;
import com.openrecordsmanager.api.template.property.ObjectPropertyTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
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
        compileMarker(root, "com.example.PluginA");

        RecordingContext context = new RecordingContext();
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{root.toUri().toURL()}, null)) {
            TemplateJsonLoader.registerAll(context, Class.forName("com.example.PluginA", true, classLoader));
        }

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
        compileMarker(root, "com.example.EmptyPlugin");

        RecordingContext context = new RecordingContext();
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{root.toUri().toURL()}, null)) {
            TemplateJsonLoader.registerAll(context, Class.forName("com.example.EmptyPlugin", true, classLoader));
        }
        assertTrue(context.ids().isEmpty());
    }

    @Test
    void onlyRegistersTemplatesFromPluginOwnJar() throws Exception {
        Path templatesRoot = tempDir.resolve("templates-src");
        writeTemplates(templatesRoot);

        Path ausGovJar = tempDir.resolve("aus-gov.jar");
        Path otherJar = tempDir.resolve("other.jar");
        buildJar(ausGovJar, "com.example.AusGov", templatesRoot);
        buildJar(otherJar, "com.example.Other", null);

        RecordingContext ausGovContext = new RecordingContext();
        RecordingContext otherContext = new RecordingContext();
        try (URLClassLoader shared = new URLClassLoader(
                new URL[]{ausGovJar.toUri().toURL(), otherJar.toUri().toURL()},
                null
        )) {
            TemplateJsonLoader.registerAll(ausGovContext, Class.forName("com.example.AusGov", true, shared));
            TemplateJsonLoader.registerAll(otherContext, Class.forName("com.example.Other", true, shared));
        }

        assertFalse(ausGovContext.ids().isEmpty());
        assertTrue(otherContext.ids().isEmpty(), "other plugin must not inherit aus-gov templates");
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

    private void buildJar(Path jar, String className, Path templatesRoot) throws Exception {
        Path classes = tempDir.resolve("classes-" + jar.getFileName());
        Files.createDirectories(classes);
        compileMarker(classes, className);
        String classPath = className.replace('.', '/') + ".class";
        byte[] classBytes = Files.readAllBytes(classes.resolve(classPath));

        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jar))) {
            jos.putNextEntry(new JarEntry(classPath));
            jos.write(classBytes);
            jos.closeEntry();
            if (templatesRoot != null) {
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
    }

    private void compileMarker(Path outputDir, String binaryName) throws Exception {
        String packageName = binaryName.substring(0, binaryName.lastIndexOf('.'));
        String simpleName = binaryName.substring(binaryName.lastIndexOf('.') + 1);
        Path srcDir = tempDir.resolve("src-" + simpleName);
        Path src = srcDir.resolve(packageName.replace('.', '/') + "/" + simpleName + ".java");
        Files.createDirectories(src.getParent());
        Files.writeString(src, "package " + packageName + "; public class " + simpleName + " {}", StandardCharsets.UTF_8);

        Process process = new ProcessBuilder(
                "javac",
                "-d", outputDir.toString(),
                src.toString()
        ).inheritIO().start();
        assertEquals(0, process.waitFor(), "javac failed for " + binaryName);
    }

    private static final class RecordingContext implements RegistrationContext {
        private final Map<String, Component> registered = new LinkedHashMap<>();

        @Override
        public void registerComponent(String id, Component component) {
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
