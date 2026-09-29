package com.openrecordsmanager.plugin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.semver4j.Semver;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class DiscoveredPluginTest {

    @TempDir
    Path tempDir;

    @Test
    void parsesValidPluginJson() throws Exception {
        DiscoveredPlugin info = DiscoveredPlugin.parseWithDescriptorFile(new ByteArrayInputStream("""
                {
                  "id":"defaults_aus_gov",
                  "version":"0.1.0"
                }
                """.getBytes(StandardCharsets.UTF_8)), null, null);
        assertEquals("defaults_aus_gov", info.id());
        assertEquals(new Semver("0.1.0"), info.version());
        assertNull(info.path());
    }

    @Test
    void rejectsMissingId() {
        assertThrows(IllegalArgumentException.class, () -> DiscoveredPlugin.parseWithDescriptorFile(new ByteArrayInputStream("""
                {
                  "version":"0.1.0"
                }
                """.getBytes(StandardCharsets.UTF_8)), null, null));
    }

    @Test
    void rejectsMissingVersion() {
        assertThrows(IllegalArgumentException.class, () -> DiscoveredPlugin.parseWithDescriptorFile(new ByteArrayInputStream("""
                {
                  "id":"demo"
                }
                """.getBytes(StandardCharsets.UTF_8)), null, null));
    }

    @Test
    void readsFromJarAndZip() throws Exception {
        Path jar = tempDir.resolve("plugin.jar");
        Path zip = tempDir.resolve("plugin.zip");
        writeArchive(jar, """
                {"id":"demo","version":"1.2.3"}
                """);
        writeArchive(zip, """
                {"id":"demo_zip","version":"2.0.0"}
                """);

        DiscoveredPlugin fromJar = DiscoveredPlugin.read(jar, null);
        assertEquals(
                new DiscoveredPlugin("demo", new Semver("1.2.3"), jar, null),
                fromJar
        );
        assertNotNull(fromJar.path());

        DiscoveredPlugin fromZip = DiscoveredPlugin.read(zip, null);
        assertEquals(
                new DiscoveredPlugin("demo_zip", new Semver("2.0.0"), zip, null),
                fromZip
        );
        assertNotNull(fromZip.path());
    }

    @Test
    void rejectsMissingPluginJson() throws Exception {
        Path jar = tempDir.resolve("empty.jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jar))) {
            jos.putNextEntry(new JarEntry("readme.txt"));
            jos.write("x".getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
        }
        assertThrows(Exception.class, () -> DiscoveredPlugin.read(jar, null));
    }

    private static void writeArchive(Path archive, String pluginJson) throws Exception {
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(archive))) {
            jos.putNextEntry(new JarEntry(DiscoveredPlugin.FILE_NAME));
            jos.write(pluginJson.getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
        }
    }
}
