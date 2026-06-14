package com.openrecordsmanager.resources;

import com.google.common.collect.ImmutableMap;
import com.openrecordsmanager.Plugin;
import com.openrecordsmanager.PluginContext;
import com.openrecordsmanager.RegisterableComponent;
import com.openrecordsmanager.auth.InputAuthProviderType;
import com.openrecordsmanager.auth.RedirectAuthProviderType;
import com.openrecordsmanager.config.ConfigDefinition;
import com.openrecordsmanager.list.ListDefinition;
import com.openrecordsmanager.property.PropertyDefinition;
import com.openrecordsmanager.recordtype.RecordTypeDefinition;
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

    private final Map<ResourceIdentifier, ConfigDefinition<?>> config;
    private final Map<ResourceIdentifier, ListDefinition> list;
    private final Map<ResourceIdentifier, PropertyDefinition<?>> property;
    private final Map<ResourceIdentifier, RecordTypeDefinition> recordType;
    private final Map<ResourceIdentifier, InputAuthProviderType> inputAuthProvider;
    private final Map<ResourceIdentifier, RedirectAuthProviderType> redirectAuthProvider;

    private final Map<ResourceType, Map<ResourceIdentifier, ? extends RegisterableComponent>> resources;

    public ResourceRegistry(PluginManager pluginManager) {
        Builder builder = new Builder();
        for (Plugin plugin : pluginManager.getPlugins()) {
            LOGGER.info("Initializing plugin {}...", plugin.getName());
            plugin.initialise(new PluginContextImpl(builder, plugin));
        }

        this.resources = ImmutableMap.of(
                ResourceType.CONFIG, this.config = builder.config.build(),
                ResourceType.LIST, this.list = builder.list.build(),
                ResourceType.PROPERTY, this.property = builder.property.build(),
                ResourceType.RECORD_TYPE, this.recordType = builder.recordType.build(),
                ResourceType.INPUT_AUTH_PROVIDER, this.inputAuthProvider = builder.inputAuthProvider.build(),
                ResourceType.REDIRECT_AUTH_PROVIDER, this.redirectAuthProvider = builder.redirectAuthProvider.build()
        );
    }

    @Nullable
    public ResourceIdentifier getResourceId(RegisterableComponent definition) {
        for (ResourceType value : ResourceType.values()) {
            Map<ResourceIdentifier, ? extends RegisterableComponent> map = resources.get(value);
            if (!map.containsValue(definition)) {
                continue;
            }

            return map.entrySet().stream()
                    .filter(entry -> Objects.equals(entry.getValue(), definition))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);
        }

        return null;
    }

    public Set<ResourceIdentifier> getIds(ResourceType type) {
        return this.resources.get(type).keySet();
    }

    public <T extends RegisterableComponent> Collection<T> getComponents(ResourceType type) {
        return (Collection<T>) this.resources.get(type).values();
    }

    public <T extends RegisterableComponent> T getComponent(ResourceType type, ResourceIdentifier id) {
        return (T) this.resources.get(type).get(id);
    }

    private static class Builder {
        private final ImmutableMap.Builder<ResourceIdentifier, ConfigDefinition<?>> config = ImmutableMap.builder();
        private final ImmutableMap.Builder<ResourceIdentifier, ListDefinition> list = ImmutableMap.builder();
        private final ImmutableMap.Builder<ResourceIdentifier, PropertyDefinition<?>> property = ImmutableMap.builder();
        private final ImmutableMap.Builder<ResourceIdentifier, RecordTypeDefinition> recordType = ImmutableMap.builder();
        private final ImmutableMap.Builder<ResourceIdentifier, InputAuthProviderType> inputAuthProvider = ImmutableMap.builder();
        private final ImmutableMap.Builder<ResourceIdentifier, RedirectAuthProviderType> redirectAuthProvider = ImmutableMap.builder();

        private void registerInstance(PluginContextImpl context, RegisterableComponent component) {
            ResourceIdentifier identifier = new ResourceIdentifier(context.plugin.getName(), component.id());

            LOGGER.info("Registering plugin resource '{}'", identifier);

            switch (component) {
                case ConfigDefinition<?> s -> this.config.put(identifier, s);
                case ListDefinition s -> this.list.put(identifier, s);
                case PropertyDefinition<?> s -> this.property.put(identifier, s);
                case RecordTypeDefinition s -> this.recordType.put(identifier, s);
                case InputAuthProviderType s -> this.inputAuthProvider.put(identifier, s);
                case RedirectAuthProviderType s -> this.redirectAuthProvider.put(identifier, s);
                default ->
                        LOGGER.error("Did not know how to register instance '{}' of type {}", identifier, component.getClass());
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
