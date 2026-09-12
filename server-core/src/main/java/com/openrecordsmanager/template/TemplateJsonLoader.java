package com.openrecordsmanager.template;

import com.openrecordsmanager.api.RegistrationContext;
import com.openrecordsmanager.api.template.TemplateComponent;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.plugin.registry.mapper.TemplateRegistrationMapper;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.Comparator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Registers JSON templates from folders named after {@link ComponentType#name()} inside a single
 * plugin artifact (the plugin class's {@link CodeSource}).
 * <p>
 * Id is the filename without {@code .json}. Scanning is scoped to that JAR/directory so a shared
 * plugin {@link ClassLoader} cannot re-register another plugin's templates.
 */
public final class TemplateJsonLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateJsonLoader.class);

    private TemplateJsonLoader() {
    }

    /**
     * Loads templates only from {@code pluginClass}'s code source (one plugin JAR or exploded root).
     */
    public static void registerAll(RegistrationContext registry, Class<?> pluginClass) {
        URL location = codeSourceLocation(pluginClass);
        if (location == null || !"file".equals(location.getProtocol())) {
            if (location != null) {
                LOGGER.warn("Unsupported plugin code source for templates: {}", location);
            }
            return;
        }
        try {
            Path path = Path.of(location.toURI());
            if (Files.isDirectory(path)) {
                registerFromDirectory(registry, path);
            } else {
                registerFromJar(registry, path);
            }
        } catch (URISyntaxException e) {
            throw new UncheckedIOException(
                    "Failed to resolve templates for " + pluginClass.getName(),
                    new IOException(e)
            );
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load templates for " + pluginClass.getName(), e);
        }
    }

    private static void registerFromDirectory(RegistrationContext registry, Path root) throws IOException {
        for (TemplateRegistrationMapper<?, ?> mapper : ComponentCatalog.TEMPLATE_MAPPERS) {
            registerFolderFromDirectory(registry, root, mapper.componentType());
        }
    }

    private static <T extends TemplateComponent> void registerFolderFromDirectory(
            RegistrationContext registry,
            Path root,
            ComponentType<T> type
    ) throws IOException {
        Path folder = root.resolve(type.name());
        if (!Files.isDirectory(folder)) {
            return;
        }
        List<Path> files;
        try (Stream<Path> paths = Files.list(folder)) {
            files = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }
        for (Path file : files) {
            String fileName = file.getFileName().toString();
            String id = fileName.substring(0, fileName.length() - ".json".length());
            try (InputStream in = Files.newInputStream(file)) {
                T component = TemplateComponent.fromJson(in, type.componentClass());
                LOGGER.info("Registering JSON template {}/{} as {}", type.name(), fileName, id);
                registry.registerComponent(id, component);
            }
        }
    }

    private static void registerFromJar(RegistrationContext registry, Path jarPath) throws IOException {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            for (TemplateRegistrationMapper<?, ?> mapper : ComponentCatalog.TEMPLATE_MAPPERS) {
                registerFolderFromJar(registry, jarFile, mapper.componentType());
            }
        }
    }

    private static <T extends TemplateComponent> void registerFolderFromJar(
            RegistrationContext registry,
            JarFile jarFile,
            ComponentType<T> type
    ) throws IOException {
        String prefix = type.name() + "/";
        List<JarEntry> entries = jarFile.stream()
                .filter(entry -> !entry.isDirectory())
                .filter(entry -> {
                    String name = entry.getName();
                    if (!name.startsWith(prefix) || !name.endsWith(".json")) {
                        return false;
                    }
                    String relative = name.substring(prefix.length());
                    return !relative.isEmpty() && !relative.contains("/");
                })
                .sorted(Comparator.comparing(JarEntry::getName))
                .toList();

        for (JarEntry entry : entries) {
            String fileName = entry.getName().substring(entry.getName().lastIndexOf('/') + 1);
            String id = fileName.substring(0, fileName.length() - ".json".length());
            try (InputStream in = jarFile.getInputStream(entry)) {
                T component = TemplateComponent.fromJson(in, type.componentClass());
                LOGGER.info("Registering JSON template {} as {}", entry.getName(), id);
                registry.registerComponent(id, component);
            }
        }
    }

    private static @Nullable URL codeSourceLocation(Class<?> pluginClass) {
        CodeSource codeSource = pluginClass.getProtectionDomain().getCodeSource();
        return codeSource != null ? codeSource.getLocation() : null;
    }
}
