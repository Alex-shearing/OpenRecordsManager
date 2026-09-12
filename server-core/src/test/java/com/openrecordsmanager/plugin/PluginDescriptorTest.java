package com.openrecordsmanager.plugin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class PluginDescriptorTest {

    @TempDir
    Path tempDir;

    @Test
    void parsesValidPluginJson() throws Exception {
        PluginDescriptor descriptor = PluginDescriptor.parse(new ByteArrayInputStream("""
                {"id":"defaults-aus-gov","version":"0.1.0"}
                """.getBytes(StandardCharsets.UTF_8)));
        assertEquals("defaults-aus-gov", descriptor.id());
        assertEquals("0.1.0", descriptor.version());
    }

    @Test
    void rejectsMissingId() {
        assertThrows(IllegalArgumentException.class, () -> PluginDescriptor.parse(new ByteArrayInputStream("""
                {"version":"0.1.0"}
                """.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void readsFromJarAndZip() throws Exception {
        Path jar = tempDir.resolve("plugin.jar");
        Path zip = tempDir.resolve("plugin.zip");
        writeArchive(jar, """
                {"id":"demo","version":"1.2.3"}
                """);
        writeArchive(zip, """
                {"id":"demo-zip","version":"2.0.0"}
                """);

        try (var jarFile = new java.util.jar.JarFile(jar.toFile())) {
            assertEquals(new PluginDescriptor("demo", "1.2.3"), PluginDescriptor.read(jarFile));
        }
        try (var zipFile = new java.util.jar.JarFile(zip.toFile())) {
            assertEquals(new PluginDescriptor("demo-zip", "2.0.0"), PluginDescriptor.read(zipFile));
        }
    }

    @Test
    void rejectsMissingPluginJson() throws Exception {
        Path jar = tempDir.resolve("empty.jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jar))) {
            jos.putNextEntry(new JarEntry("readme.txt"));
            jos.write("x".getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
        }
        try (var jarFile = new java.util.jar.JarFile(jar.toFile())) {
            assertThrows(Exception.class, () -> PluginDescriptor.read(jarFile));
        }
    }

    private static void writeArchive(Path archive, String pluginJson) throws Exception {
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(archive))) {
            jos.putNextEntry(new JarEntry(PluginDescriptor.FILE_NAME));
            jos.write(pluginJson.getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
        }
    }
}
