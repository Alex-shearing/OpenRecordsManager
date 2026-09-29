package com.openrecordsmanager.plugin.registry;

import com.google.common.collect.ImmutableMap;
import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.ComponentAccess;
import com.openrecordsmanager.api.ResourceIdentifier;

import java.util.*;
import java.util.stream.Stream;

public class ComponentRegistry<T extends Component> implements ComponentAccess.RegistryAccess<T> {
    private ImmutableMap<ResourceIdentifier, T> map = ImmutableMap.of();
    private Map<T, ResourceIdentifier> inverse = Map.of();

    @Override
    public Optional<T> get(ResourceIdentifier id) {
        return Optional.ofNullable(this.map.get(id));
    }

    @Override
    public Optional<ResourceIdentifier> getId(T definition) {
        return Optional.ofNullable(this.inverse.get(definition));
    }

    public Set<ResourceIdentifier> getIds() {
        return this.map.keySet();
    }

    public Stream<T> stream() {
        return this.map.values().stream();
    }

    public Builder builder() {
        return new Builder();
    }

    public class Builder {
        private final HashMap<ResourceIdentifier, T> builder = new HashMap<>();

        public void register(ResourceIdentifier id, T component) {
            this.builder.put(id, component);
        }

        public void build() {
            ComponentRegistry.this.map = ImmutableMap.copyOf(this.builder);
            Map<T, ResourceIdentifier> byIdentity = new IdentityHashMap<>();
            this.builder.forEach((id, component) -> byIdentity.put(component, id));
            ComponentRegistry.this.inverse = Collections.unmodifiableMap(byIdentity);
        }
    }
}
