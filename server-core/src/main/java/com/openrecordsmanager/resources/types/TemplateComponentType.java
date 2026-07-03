package com.openrecordsmanager.resources.types;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ComponentCatalog;
import com.openrecordsmanager.resources.ExpressionsService;
import com.openrecordsmanager.resources.ResourceIdentifier;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public abstract class TemplateComponentType<T extends Component, D> extends ComponentType<T> {
    public TemplateComponentType(String name, Class<T> componentClass) {
        super(name, componentClass);
    }

    /**
     * Register the component template to the database
     *
     * @param repository          the data repository
     * @param catalog             the component catalog
     * @param expressions         expressions service
     * @param id                  id to register as
     * @param definition          the definition
     * @param includeDependencies if true, all dependencies of the component will be registered before registration
     */
    public final void register(
            DataRepository repository,
            ComponentCatalog catalog,
            ExpressionsService expressions,
            ResourceIdentifier id,
            T definition,
            boolean includeDependencies
    ) {
        if (includeDependencies) {
            for (Component dependency : definition.getDependencies()) {
                TemplateComponentType<Component, ?> childType = ComponentTypes.registerableFromObject(dependency);
                if (childType == null) {
                    throw new IllegalStateException("Cannot find child resource type for dependency " + dependency);
                }
                ResourceIdentifier dependencyId = catalog.getId(childType, dependency);

                childType.register(repository, catalog, expressions, dependencyId, dependency, true);
            }
        }

        // Validate all dependencies exist
        validateAllDependenciesRegistered(definition, repository, catalog);

        this.register(repository, catalog, expressions, id, definition);
    }

    /**
     * Register the component template to the database
     *
     * @param repository  the data repository
     * @param catalog     the component catalog
     * @param expressions expressions service
     * @param id          id to register as
     * @param definition  the definition
     */
    protected abstract void register(
            DataRepository repository,
            ComponentCatalog catalog,
            ExpressionsService expressions,
            ResourceIdentifier id,
            T definition
    );

    /**
     * Get the registered instance of the component from the database
     *
     * @param id   the component id
     * @param repo the data repository
     * @return an optional instance of the registered component
     */
    public abstract Optional<D> getRegistered(ResourceIdentifier id, DataRepository repo);

    /**
     * Get the registered instance of the component from the database
     *
     * @param template the component template
     * @param repo     the data repository
     * @param catalog  the component catalog
     * @return an optional instance of the registered component
     */
    public Optional<D> getRegistered(T template, DataRepository repo, ComponentCatalog catalog) {
        ResourceIdentifier id = catalog.getId(this, template);
        if (id == null) return Optional.empty();

        return this.getRegistered(id, repo);
    }

    /**
     * Validates that all the dependencies of the component are registered, including the dependencies of dependencies.
     *
     * @param component  the component to confirm.
     * @param repository data repository
     * @param catalog    resource catalog
     */
    protected static void validateAllDependenciesRegistered(
            Component component,
            DataRepository repository,
            ComponentCatalog catalog
    ) {
        Set<Component> dependencies = collectDependencies(component, new HashSet<>());

        for (Component dependency : dependencies) {
            TemplateComponentType<Component, ?> childType = ComponentTypes.registerableFromObject(dependency);
            if (childType == null) {
                throw new IllegalStateException("Cannot find child resource type for dependency " + dependency);
            }
            Optional<?> childComponent = childType.getRegistered(dependency, repository, catalog);
            if (childComponent.isEmpty()) {
                throw new IllegalStateException("Unregistered dependency " + catalog.getId(childType, dependency));
            }
        }
    }

    /**
     * Recursively collect all dependencies of a component.
     *
     * @param component    the component to collect from
     * @param dependencies the current list of dependencies
     * @return the input set, with new dependencies added
     */
    private static Set<Component> collectDependencies(Component component, Set<Component> dependencies) {
        for (Component dependency : component.getDependencies()) {
            if (!dependencies.contains(dependency)) {
                dependencies.add(dependency);
                dependencies.addAll(collectDependencies(dependency, dependencies));
            }
        }
        return dependencies;
    }

}
