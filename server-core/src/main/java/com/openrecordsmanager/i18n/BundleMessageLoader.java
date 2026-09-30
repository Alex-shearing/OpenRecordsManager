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
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

/**
 * Loads {@code i18n/messages*.properties} from the application classpath and
 * plugin jar/zip archives.
 */
@Component
public class BundleMessageLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(BundleMessageLoader.class);

    public static final String BUNDLE_PATH = "i18n/";
    public static final String BUNDLE_PATTERN = "classpath*:i18n/messages*.properties";

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
        this.bundled.clear();
        this.reloadClasspathBundles();
        if (this.pluginManager == null) {
            return;
        }

        for (LoadedPlugin loaded : this.pluginManager.getLoadedPlugins()) {
            Path path = loaded.info().path();
            if (path != null) {
                this.loadFromArchive(path);
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
        try {
            Resource[] resources = this.resolver.getResources(BUNDLE_PATTERN);
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null) {
                    return;
                }

                Locale locale = localeFromFilename(filename);
                try (InputStream in = resource.getInputStream()) {
                    this.putProperties(in, locale);
                }
                LOGGER.debug("Loaded i18n from {} ({})", resource, locale);
            }
            LOGGER.info("Loaded {} classpath i18n bundle resource(s)", resources.length);
        } catch (IOException e) {
            LOGGER.warn("Failed to scan classpath i18n bundles", e);
        }
    }

    /**
     * Load all {@code i18n/messages*.properties} entries from a plugin jar or zip.
     * Reads the archive directly so parent classloader resources cannot shadow plugin keys.
     */
    void loadFromArchive(Path archive) {
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            long loaded = zip.stream()
                    .filter(e -> !e.isDirectory())
                    .filter(e -> e.getName().startsWith(BUNDLE_PATH))
                    .filter(e -> e.getName().endsWith(".properties"))
                    .filter(e -> e.getName().substring(BUNDLE_PATH.length()).startsWith("messages"))
                    .mapToLong(entry -> {
                        String filename = entry.getName().substring(BUNDLE_PATH.length());

                        try (InputStream in = zip.getInputStream(entry)) {
                            Locale locale = localeFromFilename(filename);
                            this.putProperties(in, locale);
                            LOGGER.debug("Loaded i18n from archive {} / {} ({})", archive, zip.getName(), locale);
                            return 1;
                        } catch (IOException e) {
                            LOGGER.error("Failed to parse bundle entry: {}", entry.getName(), e);
                            return 0;
                        }
                    })
                    .sum();

            if (loaded > 0) {
                LOGGER.info("Loaded {} i18n bundle(s) from {}", loaded, archive.getFileName());
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to load i18n bundles from {}", archive, e);
        }
    }


    private void putProperties(InputStream in, Locale locale) throws IOException {
        Properties properties = new Properties();
        try (var reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }

        Map<String, String> dest = this.bundled.computeIfAbsent(locale, _ -> new ConcurrentHashMap<>());
        properties.stringPropertyNames().forEach(name -> dest.put(name, properties.getProperty(name)));
    }

    /**
     * Map a file name to an associated locale
     * messages.properties -> en
     * messages_fr.properties -> fr
     * messages_en_AU.properties -> en-AU
     *
     * @param filename filename
     * @return the Locale extracted from the file name
     */
    static Locale localeFromFilename(String filename) throws IOException {
        if ("messages.properties".equals(filename)) {
            return Locale.ENGLISH;
        }
        if (filename.startsWith("messages_") && filename.endsWith(".properties")) {
            String tag = filename.substring("messages_".length(), filename.length() - ".properties".length());
            return Locale.forLanguageTag(tag.replace('_', '-'));
        }

        throw new IOException("could not get language code from file name " + filename);
    }
}
