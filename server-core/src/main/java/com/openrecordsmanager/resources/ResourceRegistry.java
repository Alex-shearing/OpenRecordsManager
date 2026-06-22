package com.openrecordsmanager.resources;

import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.openrecordsmanager.api.Plugin;
import com.openrecordsmanager.api.PluginContext;
import com.openrecordsmanager.api.RegisterableComponent;
import com.openrecordsmanager.api.list.ListDefinition;
import com.openrecordsmanager.resources.types.ResourceType;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class ResourceRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceRegistry.class);

    private final Table<ResourceType<?, ?>, ResourceIdentifier, RegisterableComponent> resources;

    public ResourceRegistry(PluginManager pluginManager) {
        Builder builder = new Builder();
        for (Plugin plugin : pluginManager.getPlugins()) {
            LOGGER.info("Initializing plugin {}...", plugin.getName());
            plugin.initialise(new PluginContextImpl(builder, plugin));
        }

        this.resources = builder.table.buildOrThrow();
    }

    @Nullable
    public <T extends RegisterableComponent> ResourceIdentifier getResourceId(ResourceType<T, ?> type, T definition) {
        Map<ResourceIdentifier, RegisterableComponent> values = this.resources.row(type);

        for (Map.Entry<ResourceIdentifier, RegisterableComponent> cell : values.entrySet()) {
            if (Objects.equals(cell.getValue(), definition)) {
                return cell.getKey();
            }
        }

        return null;
    }

    public Set<ResourceIdentifier> getIds(ResourceType<?, ?> type) {
        return this.resources.row(type).keySet();
    }

    @SuppressWarnings("unchecked")
    public <T extends RegisterableComponent> Collection<T> getComponents(ResourceType<T, ?> type) {
        return (Collection<T>) this.resources.row(type).values();
    }

    @SuppressWarnings("unchecked")
    public <T extends RegisterableComponent> T getComponent(ResourceType<T, ?> type, ResourceIdentifier id) {
        return (T) this.resources.get(type, id);
    }

    private static class Builder {
        private final ImmutableTable.Builder<ResourceType<?, ?>, ResourceIdentifier, RegisterableComponent> table = ImmutableTable.builder();

        private void registerInstance(PluginContextImpl context, RegisterableComponent component) {
            ResourceIdentifier identifier = new ResourceIdentifier(context.plugin.getName(), component.id());

            ResourceType<? extends RegisterableComponent, ?> type = ResourceType.fromObject(component);
            if (type == null) {
                LOGGER.error("Did not know how to register instance '{}' of type {}", identifier, component.getClass());
                return;
            }

            LOGGER.info("Registering plugin resource '{}' as {}", identifier, type);

            this.table.put(type, identifier, component);

            if (component instanceof ListDefinition def) {
                def.defaultEntries.forEach((s, listItemDef) -> {
                    registerInstance(context, listItemDef);
                });
            }
        }
    }

    private record PluginContextImpl(Builder builder, Plugin plugin) implements PluginContext {
        @Override
        public void registerComponents(RegisterableComponent... types) {
            for (RegisterableComponent type : types) {
                this.builder.registerInstance(this, type);
            }
        }
    }
}
