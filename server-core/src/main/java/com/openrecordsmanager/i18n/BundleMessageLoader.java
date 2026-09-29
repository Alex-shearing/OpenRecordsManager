package com.openrecordsmanager.i18n;

import com.openrecordsmanager.plugin.LoadedPlugin;
import com.openrecordsmanager.plugin.PluginManager;
import jakarta.annotation.PostConstruct;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Loads {@code META-INF/orm/i18n/messages*.properties} from the application classpath and
 * plugin jar/zip archives.
 */
@Component
public class BundleMessageLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(BundleMessageLoader.class);

    public static final String BUNDLE_PATH = "META-INF/orm/i18n/";
    public static final String BUNDLE_PATTERN = "classpath*:META-INF/orm/i18n/messages*.properties";

    private final @Nullable PluginManager pluginManager;

    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    private final ConcurrentHashMap<Locale, ConcurrentHashMap<String, String>> bundled = new ConcurrentHashMap<>();

    public BundleMessageLoader(@Nullable @Lazy PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    /**
     * Clears bundled messages, reloads classpath bundles, then loads each loaded plugin archive.
     */
    @PostConstruct
    public void refresh() {
        this.reloadClasspathBundles();
        if (this.pluginManager == null) {
            return;
        }
        for (LoadedPlugin loaded : this.pluginManager.getLoadedPlugins()) {
            Path path = loaded.info().path();
            if (path != null) {
                loadFromArchive(path);
            }
        }
    }

    @Nullable String findMessage(Locale locale, String key) {
        return I18nService.findMessage(this.bundled, locale, key);
    }

    Set<String> getAllKeys() {
        return this.bundled.values().stream()
                .flatMap(map -> map.keySet().stream())
                .collect(Collectors.toSet());
    }

    /**
     * Clears bundled messages and reloads every {@code messages*.properties} visible on the
     * application classpath (host + plugin-api, etc.). Does not load plugin jar classloaders —
     * use {@link #loadFromArchive(Path)} for those.
     */
    void reloadClasspathBundles() {
        this.bundled.clear();
        try {
            Resource[] resources = this.resolver.getResources(BUNDLE_PATTERN);
            for (Resource resource : resources) {
                loadResource(resource);
            }
            LOGGER.info("Loaded {} classpath i18n bundle resource(s)", resources.length);
        } catch (IOException e) {
            LOGGER.warn("Failed to scan classpath i18n bundles", e);
        }
    }

    /**
     * Load all {@code META-INF/orm/i18n/messages*.properties} entries from a plugin jar or zip.
     * Reads the archive directly so parent classloader resources cannot shadow plugin keys.
     */
    void loadFromArchive(Path archive) {
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            int loaded = 0;
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                if (!name.startsWith(BUNDLE_PATH) || !name.endsWith(".properties")) {
                    continue;
                }
                String filename = name.substring(BUNDLE_PATH.length());
                if (filename.isEmpty() || filename.contains("/")) {
                    continue;
                }
                if (!filename.startsWith("messages")) {
                    continue;
                }
                Locale locale = localeFromFilename(filename);
                try (InputStream in = zip.getInputStream(entry)) {
                    putProperties(in, locale);
                    loaded++;
                }
            }
            if (loaded > 0) {
                LOGGER.info("Loaded {} i18n bundle(s) from {}", loaded, archive.getFileName());
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to load i18n bundles from {}", archive, e);
        }
    }

    private void loadResource(Resource resource) throws IOException {
        String filename = resource.getFilename();
        if (filename == null) {
            return;
        }
        Locale locale = localeFromFilename(filename);
        try (InputStream in = resource.getInputStream()) {
            putProperties(in, locale);
        }
        LOGGER.debug("Loaded errorMessage(s) from {} ({})", resource, locale);
    }

    private void putProperties(InputStream in, Locale locale) throws IOException {
        Properties properties = new Properties();
        properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        Map<String, String> messages = new ConcurrentHashMap<>();
        for (String name : properties.stringPropertyNames()) {
            messages.put(name, properties.getProperty(name));
        }

        ConcurrentHashMap<String, String> dest = this.bundled.computeIfAbsent(
                locale,
                ignored -> new ConcurrentHashMap<>()
        );
        dest.putAll(messages);
    }

    static Locale localeFromFilename(String filename) {
        // messages.properties → en
        // messages_fr.properties → fr
        // messages_en_AU.properties → en_AU
        if ("messages.properties".equals(filename)) {
            return Locale.ENGLISH;
        }
        if (filename.startsWith("messages_") && filename.endsWith(".properties")) {
            String tag = filename.substring("messages_".length(), filename.length() - ".properties".length());
            return Locale.forLanguageTag(tag.replace('_', '-'));
        }
        return Locale.ENGLISH;
    }
}
