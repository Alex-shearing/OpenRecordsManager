package com.openrecordsmanager.resources;

import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.Plugin;
import com.openrecordsmanager.api.PluginContext;
import com.openrecordsmanager.api.list.ListDefinition;
import com.openrecordsmanager.resources.types.ComponentType;
import com.openrecordsmanager.resources.types.ComponentTypes;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ComponentCatalog {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComponentCatalog.class);

    private final Table<ComponentType<?, ?>, ResourceIdentifier, ? extends Component> components;

    public ComponentCatalog(PluginManager pluginManager) {
        Builder builder = new Builder();
        for (Plugin plugin : pluginManager.getPlugins()) {
            LOGGER.info("Initializing plugin {}...", plugin.getName());
            plugin.initialise(new PluginContextImpl(builder, plugin));
        }

        this.components = builder.table.buildOrThrow();
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends Component> ResourceIdentifier getId(ComponentType<T, ?> type, T definition) {
        Map<ResourceIdentifier, T> values = (Map<ResourceIdentifier, T>) this.components.row(type);

        for (Map.Entry<ResourceIdentifier, T> cell : values.entrySet()) {
            if (Objects.equals(cell.getValue(), definition)) {
                return cell.getKey();
            }
        }

        return null;
    }

    public Set<ResourceIdentifier> getIds(ComponentType<?, ?> type) {
        return this.components.row(type).keySet();
    }

    @SuppressWarnings("unchecked")
    public <T extends Component> Collection<T> getComponents(ComponentType<T, ?> type) {
        return (Collection<T>) this.components.row(type).values();
    }

    @SuppressWarnings("unchecked")
    public <T extends Component> Optional<T> getComponent(ComponentType<T, ?> type, ResourceIdentifier id) {
        return Optional.ofNullable((T) this.components.get(type, id));
    }

    private static class Builder {
        private final ImmutableTable.Builder<ComponentType<?, ?>, ResourceIdentifier, Component> table = ImmutableTable.builder();

        private void registerInstance(PluginContextImpl context, Component component) {
            ResourceIdentifier identifier = new ResourceIdentifier(context.plugin.getName(), component.id());

            ComponentType<? extends Component, ?> type = ComponentTypes.fromObject(component);
            if (type == null) {
                LOGGER.error("Did not know how to register instance '{}' of type {}", identifier, component.getClass());
                return;
            }

            LOGGER.info("Registering plugin component '{}' as {}", identifier, type);

            this.table.put(type, identifier, component);

            // List specific registration to all list children
            if (component instanceof ListDefinition def) {
                def.defaultEntries.forEach((_, listItemDef) -> registerInstance(context, listItemDef));
            }
        }
    }

    private record PluginContextImpl(Builder builder, Plugin plugin) implements PluginContext {
        @Override
        public void registerComponents(Component... types) {
            for (Component type : types) {
                this.builder.registerInstance(this, type);
            }
        }
    }
}
