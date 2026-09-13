package com.openrecordsmanager.plugin;

import com.openrecordsmanager.api.Plugin;
import com.openrecordsmanager.api.RegistrationContext;

/**
 * Synthetic plugin for a templates-only {@code .zip} archive (no SPI / ClassLoader entry).
 * <p>
 * JSON templates are registered by {@link com.openrecordsmanager.plugin.registry.ComponentCatalog}
 * from {@link LoadedPlugin#archive()}; {@link #initialise} is intentionally empty.
 */
public final class JsonTemplatePackPlugin implements Plugin {
    private final String id;

    public JsonTemplatePackPlugin(String id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return this.id;
    }

    @Override
    public void initialise(RegistrationContext registry) {
        // Templates loaded via LoadedPlugin.archive → TemplateJsonLoader.registerFromPath
    }
}
