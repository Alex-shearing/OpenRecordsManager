package com.openrecordsmanager.resources.types;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ComponentCatalog;
import com.openrecordsmanager.resources.ExpressionsService;
import com.openrecordsmanager.resources.ResourceIdentifier;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public abstract class ComponentType<T extends Component, D> {

    public final String name;
    private final Class<T> componentClass;

    public ComponentType(String name, Class<T> componentClass) {
        this.name = name;
        this.componentClass = componentClass;
    }

    public <K extends Component> boolean is(K object) {
        return this.componentClass.isInstance(object);
    }

    public final D register(DataRepository repository, ComponentCatalog catalog, ExpressionsService expressions, ResourceIdentifier id, T definition, boolean includeDependencies) {
        if (includeDependencies) {
            for (Component dependency : definition.getDependencies()) {
                ComponentType<Component, ?> childType = ComponentTypes.fromObject(dependency);
                if (childType == null) {
                    throw new IllegalStateException("Cannot find child resource type for dependency " + dependency);
                }
                ResourceIdentifier dependencyId = catalog.getId(childType, dependency);

                childType.register(repository, catalog, expressions, dependencyId, dependency, true);
            }
        }

        // Validate all dependencies exist
        validateAllDependenciesRegistered(definition, repository, catalog);

        return this.register(repository, catalog, expressions, id, definition);
    }

    protected abstract D register(DataRepository repository, ComponentCatalog catalog, ExpressionsService expressions, ResourceIdentifier id, T definition);

    public abstract Optional<D> getRegistered(ResourceIdentifier id, DataRepository repo);

    public Optional<T> getComponent(ResourceIdentifier id, ComponentCatalog catalog) {
        return catalog.getComponent(this, id);
    }

    /**
     * Validates that all the dependencies of the component are registered, including the dependencies of the dependencies.
     *
     * @param component  the component to confirm.
     * @param repository data repository
     * @param catalog    resource catalog
     */
    protected static void validateAllDependenciesRegistered(Component component, DataRepository repository, ComponentCatalog catalog) {
        Set<Component> dependencies = collectDependencies(component, new HashSet<>());

        for (Component dependency : dependencies) {
            ComponentType<Component, ?> childType = ComponentTypes.fromObject(dependency);
            if (childType == null) {
                throw new IllegalStateException("Cannot find child resource type for dependency " + dependency);
            }
            ResourceIdentifier dependencyId = catalog.getId(childType, dependency);

            Optional<?> childComponent = childType.getRegistered(dependencyId, repository);
            if (childComponent.isEmpty()) {
                throw new IllegalStateException("Unregistered dependency " + dependencyId);
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


    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ComponentType<?, ?> that = (ComponentType<?, ?>) o;
        return Objects.equals(componentClass, that.componentClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.componentClass.getSimpleName(), this.name);
    }

    @Override
    public String toString() {
        return this.name;
    }
}
