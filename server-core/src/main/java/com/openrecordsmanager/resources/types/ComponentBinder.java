package com.openrecordsmanager.resources.types;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ComponentCatalog;
import com.openrecordsmanager.resources.ExpressionsService;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public abstract class ComponentBinder<T extends Component, D> {

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
            for (ComponentReference<? extends Component> dependencyRef : definition.getDependencies()) {
                Component dependency = dependencyRef.getComponent(catalog);

                ComponentType<Component> childType = ComponentTypes.fromObject(dependency);
                if (childType == null) {
                    throw new IllegalStateException("Cannot find child resource type for dependency " + dependency);
                }
                ResourceIdentifier dependencyId = catalog.getId(childType, dependency);

                ComponentBinder<Component, ?> binder = ComponentBinderRegistry.get(childType);
                binder.register(repository, catalog, expressions, dependencyId, dependency, true);
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

    public Optional<D> getRegistered(ComponentReference<T> reference, ComponentCatalog catalog, DataRepository repo) {
        return this.getRegistered(reference.getId(catalog), repo);
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
        for (Component dependency : collectDependencies(catalog, component, new HashSet<>())) {
            ComponentType<Component> childType = ComponentTypes.fromObject(dependency);
            if (childType == null) {
                throw new IllegalStateException("Cannot find child resource type for dependency " + dependency);
            }

            ComponentBinder<?, ?> binder = ComponentBinderRegistry.get(childType);
            ResourceIdentifier dependencyId = catalog.getId(childType, dependency);

            Optional<?> childComponent = binder.getRegistered(dependencyId, repository);
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
    private static Set<Component> collectDependencies(ComponentCatalog catalog, Component component, Set<Component> dependencies) {
        for (ComponentReference<? extends Component> dependencyRef : component.getDependencies()) {
            Component dependency = dependencyRef.getComponent(catalog);

            if (!dependencies.contains(dependency)) {
                dependencies.add(dependency);
                dependencies.addAll(collectDependencies(catalog, dependency, dependencies));
            }
        }
        return dependencies;
    }
}
