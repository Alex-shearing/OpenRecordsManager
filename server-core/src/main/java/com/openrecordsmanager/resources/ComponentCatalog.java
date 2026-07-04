package com.openrecordsmanager.resources;

import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.openrecordsmanager.api.*;
import com.openrecordsmanager.api.template.list.ListDefinition;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.config.ConfigProperties;
import com.openrecordsmanager.config.DynamicConfigService;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ComponentCatalog implements ComponentAccess {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComponentCatalog.class);

    private Table<ComponentType<?>, ResourceIdentifier, ? extends Component> components;

    public ComponentCatalog(PluginManager pluginManager, DynamicConfigService configService) {
        this.loadCatalog(pluginManager);

        UUID defaultStore = configService.getProperty(ConfigProperties.WORKGROUP_DEFAULT_FILE_STORE);
        if (defaultStore != null && pluginManager.synchronizeWithServer(this, defaultStore)) {
            LOGGER.info("Plugin manager reported changes, reloading catalog");
            this.loadCatalog(pluginManager);
        }
    }

    private void loadCatalog(PluginManager pluginManager) {
        Builder builder = new Builder();
        for (Plugin plugin : pluginManager.getPlugins()) {
            LOGGER.info("Initializing plugin {}...", plugin.getName());
            plugin.initialise(new RegistrationContextImpl(builder, plugin));
        }

        this.components = builder.table.buildOrThrow();
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends Component> ResourceIdentifier getId(ComponentType<T> type, T definition) {
        Map<ResourceIdentifier, T> values = (Map<ResourceIdentifier, T>) this.components.row(type);

        for (Map.Entry<ResourceIdentifier, T> cell : values.entrySet()) {
            if (Objects.equals(cell.getValue(), definition)) {
                return cell.getKey();
            }
        }

        return null;
    }

    public Set<ResourceIdentifier> getIds(ComponentType<?> type) {
        return this.components.row(type).keySet();
    }

    @SuppressWarnings("unchecked")
    public <T extends Component> Collection<T> getComponents(ComponentType<T> type) {
        return (Collection<T>) this.components.row(type).values();
    }

    @SuppressWarnings("unchecked")
    public <T extends Component> Optional<T> getComponent(ComponentType<T> type, ResourceIdentifier id) {
        return Optional.ofNullable((T) this.components.get(type, id));
    }

    private static class Builder {
        private final ImmutableTable.Builder<ComponentType<?>, ResourceIdentifier, Component> table = ImmutableTable.builder();

        private void registerInstance(RegistrationContextImpl context, String id, Component component) {
            ResourceIdentifier identifier = new ResourceIdentifier(context.plugin.getName(), id);

            ComponentType<? extends Component> type = ComponentTypes.fromObject(component);
            if (type == null) {
                LOGGER.error("Did not know how to register instance '{}' of type {}", identifier, component.getClass());
                return;
            }

            LOGGER.info("Registering plugin component '{}' as {}", identifier, type);

            this.table.put(type, identifier, component);

            // List specific registration to all list children
            if (component instanceof ListDefinition def) {
                def.defaultEntries().forEach((eId, eDef) ->
                        registerInstance(context, eId, eDef)
                );
            }
        }
    }

    private record RegistrationContextImpl(Builder builder, Plugin plugin) implements RegistrationContext {
        @Override
        public void registerComponent(String id, Component component) {
            this.builder.registerInstance(this, id, component);
        }
    }
}
