package com.openrecordsmanager.plugin.registry;

import com.openrecordsmanager.api.*;
import com.openrecordsmanager.api.template.TemplateComponent;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.plugin.PluginManager;
import com.openrecordsmanager.plugin.registry.mapper.*;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ComponentCatalog implements ComponentAccess {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComponentCatalog.class);

    // Mappers
    public static final ListTemplateRegistrationMapper LIST_MAPPER = new ListTemplateRegistrationMapper();
    public static final ListElementTemplateRegistrationMapper LIST_ELEMENT_MAPPER = new ListElementTemplateRegistrationMapper();
    public static final ObjectPropertyTemplateRegistrationMapper OBJECT_PROPERTY_MAPPER = new ObjectPropertyTemplateRegistrationMapper();
    public static final RecordTypeTemplateRegistrationMapper RECORD_TYPE_MAPPER = new RecordTypeTemplateRegistrationMapper();

    private static final List<TemplateRegistrationMapper<?, ?>> TEMPLATE_MAPPERS = List.of(
            LIST_MAPPER,
            LIST_ELEMENT_MAPPER,
            OBJECT_PROPERTY_MAPPER,
            RECORD_TYPE_MAPPER
    );

    // Templates Registries
    private static final Map<ComponentType<?>, TemplateComponentRegistry<?, ?>> TEMPLATE_REGISTRIES = TEMPLATE_MAPPERS.stream()
            .map(mapper -> Map.entry(
                    mapper.componentType(),
                    new TemplateComponentRegistry<>(mapper)
            ))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


    // Static Registries
    private static final Map<ComponentType<?>, ComponentRegistry<?>> STATIC_REGISTRIES = Map.of(
            ComponentTypes.CONFIG, new ComponentRegistry<>(),
            ComponentTypes.INPUT_AUTH_PROVIDER, new ComponentRegistry<>(),
            ComponentTypes.REDIRECT_AUTH_PROVIDER, new ComponentRegistry<>(),
            ComponentTypes.FILE_STORE, new ComponentRegistry<>(),
            ComponentTypes.FILE_STORE_MIDDLEWARE, new ComponentRegistry<>()
    );

    // Combined
    private final Map<ComponentType<?>, ComponentRegistry<?>> componentsMap = Stream.concat(
                    STATIC_REGISTRIES.entrySet().stream(),
                    TEMPLATE_REGISTRIES.entrySet().stream()
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
    public <K extends TemplateComponent, D> TemplateComponentRegistry<K, D> getTemplateRegistry(
            TemplateRegistrationMapper<K, D> mapper
    ) {
        return (TemplateComponentRegistry<K, D>) TEMPLATE_REGISTRIES.get(mapper.componentType());
    }

    public Set<ComponentType<?>> getTemplateTypes() {
        return TEMPLATE_REGISTRIES.keySet();
    }

    private void loadCatalog(PluginManager pluginManager) {
        Builder builder = new Builder();
        for (Plugin plugin : pluginManager.getPlugins()) {
            LOGGER.info("Initializing plugin {}...", plugin.getName());
            plugin.initialise(new RegistrationContextImpl(builder, plugin));
        }
        builder.build();
    }

    @Nullable
    public static TemplateRegistrationMapper<?, ?> mapperFromName(String name) {
        return TEMPLATE_MAPPERS.stream()
                .filter(mapper -> mapper.componentType().name.equals(name))
                .findFirst()
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    public static <T extends TemplateComponent> TemplateRegistrationMapper<T, ?> mapperFromComponent(ComponentType<T> type) {
        return (TemplateRegistrationMapper<T, ?>) TEMPLATE_MAPPERS.stream()
                .filter(mapper -> mapper.componentType().equals(type))
                .findFirst()
                .orElseThrow();
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
