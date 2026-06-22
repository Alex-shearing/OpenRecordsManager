package com.openrecordsmanager.resources.types;

import com.openrecordsmanager.api.RegisterableComponent;
import com.openrecordsmanager.model.repositories.DataRepository;
import com.openrecordsmanager.resources.ExpressionsService;
import com.openrecordsmanager.resources.ResourceIdentifier;
import com.openrecordsmanager.resources.ResourceRegistry;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public abstract class ResourceType<T extends RegisterableComponent, D> {

    private final String name;
    private final Class<T> componentClass;

    public ResourceType(String name, Class<T> componentClass) {
        this.name = name;
        this.componentClass = componentClass;
    }

    public <K extends RegisterableComponent> boolean is(K object) {
        return this.componentClass.isInstance(object);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <K extends RegisterableComponent, D> ResourceType<K, D> fromObject(K object) {
        for (ResourceType<?, ?> value : ResourceTypes.VALUES) {
            if (value.is(object)) {
                return (ResourceType<K, D>) value;
            }
        }

        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ResourceType<?, ?> that = (ResourceType<?, ?>) o;
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

    public D register(DataRepository repository, ResourceRegistry registry, ExpressionsService expressions, ResourceIdentifier id, T definition, boolean includeDependencies) {
        if (includeDependencies) {
            for (RegisterableComponent dependency : definition.getDependencies()) {
                ResourceType<RegisterableComponent, ?> childType = fromObject(dependency);
                if (childType == null) {
                    throw new IllegalStateException("Cannot find child resource type for dependency " + dependency);
                }
                ResourceIdentifier dependencyId = registry.getResourceId(childType, dependency);

                childType.register(repository, registry, expressions, dependencyId, dependency, true);
            }
        } else {
            // Validate all dependencies exist
            for (RegisterableComponent dependency : definition.getDependencies()) {
                ResourceType<RegisterableComponent, ?> childType = fromObject(dependency);
                if (childType == null) {
                    throw new IllegalStateException("Cannot find child resource type for dependency " + dependency);
                }
                ResourceIdentifier dependencyId = registry.getResourceId(childType, dependency);

                if (childType.get(dependencyId, repository).isEmpty()) {
                    throw new IllegalStateException("Unregistered dependency " + dependencyId);
                }
            }
        }
        return this.register(repository, registry, expressions, id, definition);
    }

    protected abstract D register(DataRepository repository, ResourceRegistry registry, ExpressionsService expressions, ResourceIdentifier id, T definition);

    protected abstract Optional<D> get(ResourceIdentifier id, DataRepository repo);
}
