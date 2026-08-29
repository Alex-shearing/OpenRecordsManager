package com.openrecordsmanager;

import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.config.ConfigService;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Serves a static SPA from {@code server.web-directory} when configured
 * (e.g. {@code ./static} after unpacking the web zip next to the JAR).
 */
@Configuration
public class WebServerMvcConfigurer implements WebMvcConfigurer {

    @Nullable
    private final String webDir;

    public WebServerMvcConfigurer(ConfigService configService) {
        Path path = Path.of(configService.getOrThrow(BuiltinConfigs.WEB_DIRECTORY));
        if (isDirectoryAndNotEmpty(path)) {
            this.webDir = path.toAbsolutePath().toUri().toString();
        } else {
            this.webDir = null;
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (StringUtils.isBlank(this.webDir)) {
            return;
        }

        registry.addResourceHandler("/**")
                .addResourceLocations(this.webDir)
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);

                        if ((requestedResource.exists() && requestedResource.isReadable())
                                || resourcePath.startsWith("api/")) {
                            return requestedResource;
                        }

                        return location.createRelative("index.html");
                    }
                });
    }

    public static boolean isDirectoryAndNotEmpty(Path path) {
        try {
            if (Files.isDirectory(path)) {
                try (Stream<Path> entries = Files.list(path)) {
                    return entries.findAny().isPresent();
                }
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }
}
