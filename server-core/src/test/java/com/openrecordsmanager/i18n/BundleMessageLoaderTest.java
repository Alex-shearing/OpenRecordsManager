package com.openrecordsmanager.i18n;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BundleMessageLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void loadFromArchiveReadsPluginMessagesIgnoringParentClasspathShadowing() throws Exception {
        Path jar = tempDir.resolve("demo-plugin.jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jar))) {
            jos.putNextEntry(new JarEntry("META-INF/orm/i18n/messages.properties"));
            jos.write("""
                    plugin.demo.name=Demo Plugin
                    plugin.demo.description=From the plugin jar
                    """.getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
        }

        BundleMessageLoader loader = new BundleMessageLoader(null);
        loader.reloadClasspathBundles();
        loader.loadFromArchive(jar);

        assertEquals("Demo Plugin", loader.findMessage(Locale.ENGLISH, "plugin.demo.name"));
        assertEquals("From the plugin jar", loader.findMessage(Locale.ENGLISH, "plugin.demo.description"));
    }

    @Test
    void reloadClasspathBundlesClearsPreviouslyLoadedArchiveKeys() throws Exception {
        Path jar = tempDir.resolve("ephemeral.jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jar))) {
            jos.putNextEntry(new JarEntry("META-INF/orm/i18n/messages.properties"));
            jos.write("plugin.ephemeral.name=Ephemeral\n".getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
        }

        BundleMessageLoader loader = new BundleMessageLoader(null);
        loader.loadFromArchive(jar);
        assertEquals("Ephemeral", loader.findMessage(Locale.ENGLISH, "plugin.ephemeral.name"));

        loader.reloadClasspathBundles();
        assertNull(loader.findMessage(Locale.ENGLISH, "plugin.ephemeral.name"));
    }

    @Test
    void reloadClasspathBundlesIncludesWebUiMessages() {
        BundleMessageLoader loader = new BundleMessageLoader(null);
        loader.reloadClasspathBundles();

        assertEquals("Admin", loader.findMessage(Locale.ENGLISH, "web.nav.admin"));
        assertEquals("Audit comment", loader.findMessage(Locale.ENGLISH, "web.common.audit_comment"));
    }
}
