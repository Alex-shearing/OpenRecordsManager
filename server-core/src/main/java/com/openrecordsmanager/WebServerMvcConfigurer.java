package com.openrecordsmanager;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class WebServerMvcConfigurer implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
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
                        return new ClassPathResource("/static/index.html");
                    }
                });
    }
}
