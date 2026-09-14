package com.openrecordsmanager.plugin;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.Plugin;
import com.openrecordsmanager.api.RegistrationContext;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.template.TemplateJsonLoader;

/**
 * A {@link Plugin} instance currently loaded by {@link PluginManager}, with its on-disk archive
 * when loaded from the plugins directory (JAR or ZIP).
 *
 * @param info   info loaded from the local plugin file
 * @param plugin runtime plugin instance
 */
public record LoadedPlugin(LocalPluginInfo info, Plugin plugin) {

    public void initialize(ComponentCatalog.Builder builder) {
        RegistrationContextImpl context = new RegistrationContextImpl(builder, this.info.id());

        if (this.info.path() != null) {
            TemplateJsonLoader.registerFromPath(context, this.info.path());
        }
        this.plugin.initialise(context);
    }

    public String id() {
        return this.info.id();
    }

    private record RegistrationContextImpl(ComponentCatalog.Builder builder, String pluginId)
            implements RegistrationContext {

        @Override
        public String id() {
            return this.pluginId;
        }

        @Override
        public void registerComponent(String id, Component component) {
            this.builder.registerInstance(this, id, component);
        }
    }
}
