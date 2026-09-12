package com.openrecordsmanager.plugin;

import com.openrecordsmanager.api.Plugin;
import com.openrecordsmanager.api.RegistrationContext;
import com.openrecordsmanager.template.TemplateJsonLoader;

import java.nio.file.Path;

/**
 * Synthetic plugin for a templates-only {@code .zip} archive (no SPI / ClassLoader entry).
 */
public final class JsonTemplatePackPlugin implements Plugin {
    private final String id;
    private final Path archivePath;

    public JsonTemplatePackPlugin(String id, Path archivePath) {
        this.id = id;
        this.archivePath = archivePath;
    }

    @Override
    public String getName() {
        return this.id;
    }

    @Override
    public void initialise(RegistrationContext registry) {
        TemplateJsonLoader.registerFromPath(registry, this.archivePath);
    }
}
