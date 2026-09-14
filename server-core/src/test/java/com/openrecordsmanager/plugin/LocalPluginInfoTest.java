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

class LocalPluginInfoTest {

    @TempDir
    Path tempDir;

    @Test
    void parsesValidPluginJson() throws Exception {
        LocalPluginInfo info = LocalPluginInfo.parseWithDescriptorFile(new ByteArrayInputStream("""
                {
                  "id":"defaults_aus_gov",
                  "version":"0.1.0",
                  "displayName":"Australian Government Defaults",
                  "description":"Default lists and record types for Australian Government protective markings."
                }
                """.getBytes(StandardCharsets.UTF_8)), null);
        assertEquals("defaults_aus_gov", info.id());
        assertEquals("0.1.0", info.version());
        assertEquals("Australian Government Defaults", info.displayName());
        assertEquals(
                "Default lists and record types for Australian Government protective markings.",
                info.description()
        );
        assertNull(info.path());
    }

    @Test
    void rejectsMissingId() {
        assertThrows(IllegalArgumentException.class, () -> LocalPluginInfo.parseWithDescriptorFile(new ByteArrayInputStream("""
                {
                  "version":"0.1.0",
                  "displayName":"Demo",
                  "description":"A demo plugin"
                }
                """.getBytes(StandardCharsets.UTF_8)), null));
    }

    @Test
    void rejectsMissingDisplayName() {
        assertThrows(IllegalArgumentException.class, () -> LocalPluginInfo.parseWithDescriptorFile(new ByteArrayInputStream("""
                {
                  "id":"demo",
                  "version":"0.1.0",
                  "description":"A demo plugin"
                }
                """.getBytes(StandardCharsets.UTF_8)), null));
    }

    @Test
    void readsFromJarAndZip() throws Exception {
        Path jar = tempDir.resolve("plugin.jar");
        Path zip = tempDir.resolve("plugin.zip");
        writeArchive(jar, """
                {"id":"demo","version":"1.2.3","displayName":"Demo","description":"Demo jar plugin"}
                """);
        writeArchive(zip, """
                {"id":"demo_zip","version":"2.0.0","displayName":"Demo Zip","description":"Demo zip plugin"}
                """);

        LocalPluginInfo fromJar = LocalPluginInfo.read(jar);
        assertEquals(
                new LocalPluginInfo("demo", "1.2.3", "Demo", "Demo jar plugin", jar),
                fromJar
        );
        assertNotNull(fromJar.path());

        LocalPluginInfo fromZip = LocalPluginInfo.read(zip);
        assertEquals(
                new LocalPluginInfo("demo_zip", "2.0.0", "Demo Zip", "Demo zip plugin", zip),
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
        assertThrows(Exception.class, () -> LocalPluginInfo.read(jar));
    }

    private static void writeArchive(Path archive, String pluginJson) throws Exception {
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(archive))) {
            jos.putNextEntry(new JarEntry(LocalPluginInfo.FILE_NAME));
            jos.write(pluginJson.getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
        }
    }
}
