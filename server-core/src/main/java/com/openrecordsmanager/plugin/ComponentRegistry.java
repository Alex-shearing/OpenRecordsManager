package com.openrecordsmanager.plugin;

import com.google.common.collect.ImmutableBiMap;
import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.ResourceIdentifier;

import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class ComponentRegistry<T extends Component> {
    private ImmutableBiMap<ResourceIdentifier, T> map = ImmutableBiMap.of();

    public Optional<T> getComponent(ResourceIdentifier id) {
        return Optional.ofNullable(this.map.get(id));
    }

    public Optional<ResourceIdentifier> getId(T definition) {
        return Optional.ofNullable(this.map.inverse().get(definition));
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
            ComponentRegistry.this.map = ImmutableBiMap.copyOf(this.builder);
        }
    }
}
