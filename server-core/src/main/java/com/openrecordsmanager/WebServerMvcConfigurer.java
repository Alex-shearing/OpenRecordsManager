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

    private final String webDir;

    public WebServerMvcConfigurer(@Value("${server.web.directory}") String webDir) {
        this.webDir = webDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String webPathAbsolute = Paths.get(this.webDir).toAbsolutePath().toUri().toString();

        registry.addResourceHandler("/**")
                .addResourceLocations(webPathAbsolute)
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);

                        // If the browser requests a real stream (like css, js, images) or an API endpoint, serve it normally
                        if (requestedResource.exists() && requestedResource.isReadable() || resourcePath.startsWith("api/")) {
                            return requestedResource;
                        }

                        return location.createRelative("index.html");
                    }
                });
    }
}
