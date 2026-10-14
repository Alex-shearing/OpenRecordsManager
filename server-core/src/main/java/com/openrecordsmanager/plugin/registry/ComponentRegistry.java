package com.openrecordsmanager.plugin.registry;

import com.google.common.collect.ImmutableMap;
import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.ComponentAccess;
import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.types.ComponentType;

import java.util.*;
import java.util.stream.Stream;

public class ComponentRegistry<T extends Component> implements ComponentAccess.RegistryAccess<T> {
    private Map<ResourceIdentifier, T> map = Map.of();
    private Map<T, ResourceIdentifier> inverse = Map.of();

    protected final ComponentType<T> type;

    public ComponentRegistry(ComponentType<T> type) {
        this.type = type;
    }

    @Override
    public Optional<T> get(ResourceIdentifier id) {
        return Optional.ofNullable(this.map.get(id));
    }

    @Override
    public Optional<ResourceIdentifier> getId(T definition) {
        return Optional.ofNullable(this.inverse.get(definition));
    }

    public Stream<ComponentReference<T>> references() {
        return this.map.keySet().stream()
                .map(id -> ComponentReference.of(this.type, id));
    }

    public Stream<T> stream() {
        return this.map.values().stream();
    }

    public Builder builder() {
        return new Builder();
    }

    public class Builder {
        private final HashMap<ResourceIdentifier, T> builder = new HashMap<>();

        public ComponentReference<T> register(ResourceIdentifier id, T component) {
            this.builder.put(id, component);
            return ComponentReference.of(ComponentRegistry.this.type, id);
        }

        public void build() {
            ComponentRegistry.this.map = ImmutableMap.copyOf(this.builder);
            Map<T, ResourceIdentifier> byIdentity = new IdentityHashMap<>();
            this.builder.forEach((id, component) -> byIdentity.put(component, id));
            ComponentRegistry.this.inverse = Collections.unmodifiableMap(byIdentity);
        }
    }
}
