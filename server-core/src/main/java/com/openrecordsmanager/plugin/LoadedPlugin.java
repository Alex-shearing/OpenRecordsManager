package com.openrecordsmanager.plugin;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.Plugin;
import com.openrecordsmanager.api.RegistrationContext;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.template.TemplateJsonLoader;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

/**
 * A {@link Plugin} instance currently loaded by {@link PluginManager}, with its on-disk archive
 * when loaded from the plugins directory (JAR or ZIP).
 */
public record LoadedPlugin(Plugin plugin, @Nullable Path archive) {
    public String name() {
        return this.plugin.getName();
    }

    public void initialize(ComponentCatalog.Builder builder) {
        RegistrationContextImpl context = new RegistrationContextImpl(builder, this.plugin);

        if (this.archive != null) {
            TemplateJsonLoader.registerFromPath(context, this.archive);
        }
        this.plugin.initialise(context);
    }

    private record RegistrationContextImpl(ComponentCatalog.Builder builder,
                                           Plugin plugin) implements RegistrationContext {

        @Override
        public String getName() {
            return plugin.getName();
        }

        @Override
        public void registerComponent(String id, Component component) {
            this.builder.registerInstance(this, id, component);
        }
    }
}
