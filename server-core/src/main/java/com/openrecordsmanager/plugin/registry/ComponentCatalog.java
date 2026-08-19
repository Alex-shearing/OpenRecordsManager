package com.openrecordsmanager.plugin.registry;

import com.openrecordsmanager.api.*;
import com.openrecordsmanager.api.template.TemplateComponent;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.plugin.PluginManager;
import com.openrecordsmanager.plugin.registry.mapper.ListElementTemplateRegistrationMapper;
import com.openrecordsmanager.plugin.registry.mapper.ListTemplateRegistrationMapper;
import com.openrecordsmanager.plugin.registry.mapper.ObjectPropertyTemplateRegistrationMapper;
import com.openrecordsmanager.plugin.registry.mapper.RecordTypeTemplateRegistrationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ComponentCatalog implements ComponentAccess {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComponentCatalog.class);

    // Static
    private final Map<ComponentType<?>, ComponentRegistry<?>> staticRegistries = Map.of(
            ComponentTypes.CONFIG, new ComponentRegistry<>(),
            ComponentTypes.INPUT_AUTH_PROVIDER, new ComponentRegistry<>(),
            ComponentTypes.REDIRECT_AUTH_PROVIDER, new ComponentRegistry<>(),
            ComponentTypes.FILE_STORE, new ComponentRegistry<>(),
            ComponentTypes.FILE_STORE_MIDDLEWARE, new ComponentRegistry<>()
    );

    // Templates
    private final Map<ComponentType<?>, TemplateComponentRegistry<?, ?>> templateRegistries = Map.of(
            ComponentTypes.LIST, new TemplateComponentRegistry<>(new ListTemplateRegistrationMapper()),
            ComponentTypes.LIST_ELEMENT, new TemplateComponentRegistry<>(new ListElementTemplateRegistrationMapper()),
            ComponentTypes.OBJECT_PROPERTY, new TemplateComponentRegistry<>(new ObjectPropertyTemplateRegistrationMapper()),
            ComponentTypes.RECORD_TYPE, new TemplateComponentRegistry<>(new RecordTypeTemplateRegistrationMapper())
    );

    // Combined
    private final Map<ComponentType<?>, ComponentRegistry<?>> componentsMap = Stream.concat(
                    staticRegistries.entrySet().stream(),
                    templateRegistries.entrySet().stream()
            )
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    public ComponentCatalog(
            PluginManager pluginManager,
            @Value("${workgroup.default_file_store:#{null}}") Optional<UUID> defaultStore
    ) {
        this.loadCatalog(pluginManager);

        if (defaultStore.isPresent() && pluginManager.synchronizeWithServer(this, defaultStore.get())) {
            LOGGER.info("Plugin manager reported changes, reloading catalog");
            this.loadCatalog(pluginManager);
        }
    }

    @SuppressWarnings("unchecked")
    public <K extends Component> ComponentRegistry<K> getRegistry(ComponentType<K> type) {
        return (ComponentRegistry<K>) this.componentsMap.get(type);
    }

    @SuppressWarnings("unchecked")
    public <K extends TemplateComponent, D> TemplateComponentRegistry<K, D> getTemplateRegistry(ComponentType<K> type) {
        return (TemplateComponentRegistry<K, D>) this.templateRegistries.get(type);
    }

    public Set<ComponentType<?>> getTemplateTypes() {
        return this.templateRegistries.keySet();
    }

    private void loadCatalog(PluginManager pluginManager) {
        Builder builder = new Builder();
        for (Plugin plugin : pluginManager.getPlugins()) {
            LOGGER.info("Initializing plugin {}...", plugin.getName());
            plugin.initialise(new RegistrationContextImpl(builder, plugin));
        }
        builder.build();
    }

    private class Builder {
        private final Map<ComponentType<?>, ComponentRegistry<?>.Builder> builder = ComponentCatalog.this.componentsMap
                .entrySet().stream()
                .map(a -> Map.entry(a.getKey(), a.getValue().builder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        @SuppressWarnings("unchecked")
        private <T extends Component> void registerInstance(RegistrationContextImpl context, String id, T component) {
            ResourceIdentifier identifier = new ResourceIdentifier(context.plugin.getName(), id);

            ComponentType<T> type = ComponentTypes.fromObject(component);
            ComponentRegistry<T>.Builder typeBuilder = (ComponentRegistry<T>.Builder) this.builder.get(type);

            LOGGER.info("Registering plugin component '{}' as {}", identifier, type);

            typeBuilder.register(identifier, component);
        }

        public void build() {
            this.builder.forEach((_, b) -> b.build());
        }
    }

    private record RegistrationContextImpl(Builder builder, Plugin plugin) implements RegistrationContext {
        @Override
        public void registerComponent(String id, Component component) {
            this.builder.registerInstance(this, id, component);
        }
    }
}
