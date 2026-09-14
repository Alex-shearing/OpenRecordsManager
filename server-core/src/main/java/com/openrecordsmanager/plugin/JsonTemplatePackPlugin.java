package com.openrecordsmanager.plugin;

import com.openrecordsmanager.api.Plugin;
import com.openrecordsmanager.api.RegistrationContext;

/**
 * Synthetic plugin for a templates-only {@code .zip} archive (no SPI / ClassLoader entry).
 * <p>
 * JSON templates are registered by {@link com.openrecordsmanager.plugin.registry.ComponentCatalog}
 * from the zip file.
 */
public final class JsonTemplatePackPlugin implements Plugin {
    @Override
    public void initialise(RegistrationContext registry) {
        // Templates loaded via LoadedPlugin.archive → TemplateJsonLoader.registerFromPath
    }
}
