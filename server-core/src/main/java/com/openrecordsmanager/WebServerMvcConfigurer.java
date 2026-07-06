package com.openrecordsmanager;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.nio.file.Paths;

@Configuration
public class WebServerMvcConfigurer implements WebMvcConfigurer {

    private final String directory;

    public WebServerMvcConfigurer(@Value("${server.web.directory}") String directory) {
        this.directory = directory;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String currentDir = Paths.get(this.directory).toAbsolutePath().toUri().toString();

        registry.addResourceHandler("/**")
                .addResourceLocations(currentDir)
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);

                        // If the browser requests a real file (like css, js, images) or an API endpoint, serve it normally
                        if (requestedResource.exists() && requestedResource.isReadable() || resourcePath.startsWith("api/")) {
                            return requestedResource;
                        }

                        // Otherwise, route back to Svelte's index.html so Svelte's router can handle it
                        // We must return a Resource object that points *inside* the file system handler
                        // that is currently active for this registry (the one pointing to externalStaticPath).
                        // It's safer to reference 'location' contextually here if possible, but for simplicity:
                        return location.createRelative("index.html");
                    }
                });
    }
}
