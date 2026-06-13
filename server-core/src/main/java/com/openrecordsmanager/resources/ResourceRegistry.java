package com.openrecordsmanager.resources;

import com.google.common.collect.ImmutableMap;
import com.openrecordsmanager.Plugin;
import com.openrecordsmanager.PluginResourceRegistry;
import com.openrecordsmanager.RegisterableComponent;
import com.openrecordsmanager.auth.InputAuthProviderType;
import com.openrecordsmanager.auth.RedirectAuthProviderType;
import com.openrecordsmanager.config.ConfigProperty;
import com.openrecordsmanager.list.ListDefinition;
import com.openrecordsmanager.property.PropertyDefinition;
import com.openrecordsmanager.recordtype.RecordTypeDefinition;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ResourceRegistry<T extends RegisterableComponent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceRegistry.class);

    private final List<ConfigProperty<?>> configProperties = new ArrayList<>();
    private final Map<ResourceType, Map<ResourceIdentifier, T>> resources = buildMap();

    public ResourceRegistry(PluginManager pluginManager) {
        for (Plugin plugin : pluginManager.getPlugins()) {
            LOGGER.info("Initializing plugin {}...", plugin.getName());
            plugin.initialise(new PluginImpl(plugin.getName()));
        }
    }

    @Nullable
    public ResourceIdentifier getResourceId(RegisterableComponent definition) {
        for (ResourceType value : ResourceType.values()) {
            Map<ResourceIdentifier, T> map = resources.get(value);
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

    public List<ConfigProperty<?>> getConfigProperties() {
        return configProperties;
    }

    public Map<ResourceIdentifier, ? extends RegisterableComponent> getResources(ResourceType type) {
        return this.resources.get(type);
    }

    @SuppressWarnings("unchecked")
    public Map<ResourceIdentifier, InputAuthProviderType> getInputAuthProviders() {
        return (Map<ResourceIdentifier, InputAuthProviderType>) this.getResources(ResourceType.INPUT_AUTH_PROVIDER);
    }

    @SuppressWarnings("unchecked")
    public Map<ResourceIdentifier, RedirectAuthProviderType> getRedirectAuthProviders() {
        return (Map<ResourceIdentifier, RedirectAuthProviderType>) this.getResources(ResourceType.REDIRECT_AUTH_PROVIDER);
    }

    @SuppressWarnings("unchecked")
    public Map<ResourceIdentifier, ListDefinition> getLists() {
        return (Map<ResourceIdentifier, ListDefinition>) this.getResources(ResourceType.LIST);
    }

    @SuppressWarnings("unchecked")
    public Map<ResourceIdentifier, PropertyDefinition<?>> getProperties() {
        return (Map<ResourceIdentifier, PropertyDefinition<?>>) this.getResources(ResourceType.PROPERTY);
    }

    @SuppressWarnings("unchecked")
    public Map<ResourceIdentifier, RecordTypeDefinition> getRecordTypes() {
        return (Map<ResourceIdentifier, RecordTypeDefinition>) this.getResources(ResourceType.RECORD_TYPE);
    }

    private void registerInstance(PluginImpl plugin, T component) {
        ResourceIdentifier identifier = new ResourceIdentifier(plugin.name, component.id());

        boolean registered = false;
        for (ResourceType value : ResourceType.values()) {
            if (value.isOf(component)) {
                this.resources.get(value).put(identifier, component);
                registered = true;
                break;
            }
        }

        if (!registered) {
            LOGGER.error("Did not know how to register instance '{}' of type {}", identifier, component.getClass());
        }

//        switch (component) {
//            case ListDefinition listDefinition -> this.lists.put(identifier, listDefinition);
//            case PropertyDefinition<?> property -> this.properties.put(identifier, property);
//            case RecordTypeDefinition recordType -> this.recordTypes.put(identifier, recordType);
//            case InputAuthProviderType recordType -> this.inputAuthProviderTypes.put(identifier, recordType);
//            case RedirectAuthProviderType recordType -> this.redirectAuthProviderTypes.put(identifier, recordType);
//            default ->
//                    LOGGER.error("Did not know how to register instance '{}' of type {}", identifier, component.getClass());
//        }
    }

    private Map<ResourceType, Map<ResourceIdentifier, T>> buildMap() {
        ImmutableMap.Builder<ResourceType, Map<ResourceIdentifier, T>> builder = ImmutableMap.builder();
        for (ResourceType value : ResourceType.values()) {
            builder.put(value, new HashMap<>());
        }
        return builder.build();
    }

    private class PluginImpl implements PluginResourceRegistry {
        private final String name;

        public PluginImpl(String name) {
            this.name = name;
        }

        @Override
        public void registerConfig(ConfigProperty<?> property) {
            ResourceRegistry.this.configProperties.add(property);
        }

        @Override
        public void registerInstanceComponents(RegisterableComponent... types) {
            for (RegisterableComponent type : types) {
                registerInstance(this, (T) type);
            }
        }
    }
}
