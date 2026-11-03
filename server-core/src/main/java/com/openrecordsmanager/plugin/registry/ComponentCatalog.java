package com.openrecordsmanager.plugin.registry;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.RegistrationContext;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.TemplateComponent;
import com.openrecordsmanager.api.template.list.ListElementTemplate;
import com.openrecordsmanager.api.template.list.ListTemplate;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.plugin.LoadedPlugin;
import com.openrecordsmanager.plugin.PluginManager;
import com.openrecordsmanager.plugin.registry.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ComponentCatalog {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComponentCatalog.class);

    // Mappers
    public static final ListTemplateRegistrationMapper LIST_MAPPER = new ListTemplateRegistrationMapper();
    public static final ListElementTemplateRegistrationMapper LIST_ELEMENT_MAPPER = new ListElementTemplateRegistrationMapper();
    public static final ObjectPropertyTemplateRegistrationMapper OBJECT_PROPERTY_MAPPER = new ObjectPropertyTemplateRegistrationMapper();
    public static final RecordTypeTemplateRegistrationMapper RECORD_TYPE_MAPPER = new RecordTypeTemplateRegistrationMapper();
    public static final LocationTypeTemplateRegistrationMapper LOCATION_TYPE_MAPPER =
            new LocationTypeTemplateRegistrationMapper();
    public static final RelationshipTypeTemplateRegistrationMapper RELATIONSHIP_TYPE_MAPPER =
            new RelationshipTypeTemplateRegistrationMapper();

    public static final List<TemplateRegistrationMapper<?, ?>> TEMPLATE_MAPPERS = List.of(
            LIST_MAPPER,
            LIST_ELEMENT_MAPPER,
            OBJECT_PROPERTY_MAPPER,
            RECORD_TYPE_MAPPER,
            LOCATION_TYPE_MAPPER,
            RELATIONSHIP_TYPE_MAPPER
    );

    // Templates Registries
    private static final Map<ComponentType<?>, TemplateComponentRegistry<?, ?>> TEMPLATE_REGISTRIES = TEMPLATE_MAPPERS.stream()
            .map(mapper -> Map.entry(
                    mapper.componentType(),
                    new TemplateComponentRegistry<>(mapper)
            ))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


    // Static Registries
    private static final Map<ComponentType<?>, ComponentRegistry<?>> STATIC_REGISTRIES = Stream.of(
            ComponentTypes.CONFIG,
            ComponentTypes.INPUT_AUTH_PROVIDER,
            ComponentTypes.REDIRECT_AUTH_PROVIDER,
            ComponentTypes.FILE_STORE,
            ComponentTypes.FILE_STORE_MIDDLEWARE,
            ComponentTypes.ACTION,
            ComponentTypes.SEARCH_FIELD_PROVIDER
    ).collect(Collectors.toMap(Function.identity(), ComponentRegistry::new));

    // Combined
    private final Map<ComponentType<?>, ComponentRegistry<?>> componentsMap = Stream.concat(
                    STATIC_REGISTRIES.entrySet().stream(),
                    TEMPLATE_REGISTRIES.entrySet().stream()
            )
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    public ComponentCatalog(PluginManager pluginManager) {
        this.reload(pluginManager);
    }

    public void reload(PluginManager pluginManager) {
        Builder builder = new Builder();
        for (LoadedPlugin loaded : pluginManager.getLoadedPlugins()) {
            LOGGER.info("Initializing plugin {}...", loaded.id());
            loaded.initialize(builder);
        }
        builder.build();
    }

    @SuppressWarnings("unchecked")
    public <K extends Component> ComponentRegistry<K> getRegistry(ComponentType<K> type) {
        return (ComponentRegistry<K>) this.componentsMap.get(type);
    }

    public <T extends Component> Optional<T> get(ComponentReference<T> reference) {
        return this.getRegistry(reference.type()).get(reference.id());
    }

    @SuppressWarnings("unchecked")
    public <K extends TemplateComponent, D> TemplateComponentRegistry<K, D> getTemplateRegistry(
            TemplateRegistrationMapper<K, D> mapper
    ) {
        return (TemplateComponentRegistry<K, D>) TEMPLATE_REGISTRIES.get(mapper.componentType());
    }

    @SuppressWarnings("unchecked")
    public static <T extends TemplateComponent> TemplateRegistrationMapper<T, ?> mapperFromComponent(ComponentType<T> type) {
        return (TemplateRegistrationMapper<T, ?>) TEMPLATE_MAPPERS.stream()
                .filter(mapper -> mapper.componentType().equals(type))
                .findFirst()
                .orElseThrow();
    }

    public class Builder {
        private final Map<ComponentType<?>, ComponentRegistry<?>.Builder> builder = ComponentCatalog.this.componentsMap
                .entrySet().stream()
                .map(a -> Map.entry(a.getKey(), a.getValue().builder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        @SuppressWarnings("unchecked")
        public <T extends Component> ComponentReference<T> registerInstance(RegistrationContext context, String id, T component) {
            ResourceIdentifier identifier = new ResourceIdentifier(context.id(), id);

            ComponentType<T> type = ComponentTypes.fromObject(component);
            ComponentRegistry<T>.Builder typeBuilder = (ComponentRegistry<T>.Builder) this.builder.get(type);

            LOGGER.info("Registering plugin component '{}' as {}", identifier, type);

            ComponentReference<T> ref = typeBuilder.register(identifier, component);

            if (component instanceof ListTemplate(Map<String, ListElementTemplate> defaultEntries)) {
                ComponentReference<ListTemplate> parentRef = (ComponentReference<ListTemplate>) ref;

                // Replace each default entry with a version that references the parent
                defaultEntries.replaceAll((_, element) -> element.withParent(parentRef));

                // Register all the default entries
                defaultEntries.forEach((s, element) -> this.registerInstance(context, s, element));
            }

            return ref;
        }

        public void build() {
            this.builder.forEach((_, b) -> b.build());
        }
    }
}
