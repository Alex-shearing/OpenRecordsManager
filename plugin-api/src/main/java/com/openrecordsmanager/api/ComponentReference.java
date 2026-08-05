package com.openrecordsmanager.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.api.types.ComponentTypes;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.KeyDeserializer;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public abstract class ComponentReference<T extends Component> {

    public abstract Optional<T> getComponent(ComponentAccess catalog);

    public abstract Optional<ResourceIdentifier> getId(ComponentAccess catalog);

    public abstract ComponentType<T> getType();

    public static <K extends Component> ComponentReference<K> of(K value) {
        return new Value<>(value);
    }

    public static <K extends Component> ComponentReference<K> of(ComponentType<K> type, ResourceIdentifier id) {
        return new Reference<>(type, id);
    }

    @JsonCreator
    public static ComponentReference<? extends Component> valueOf(String fqn) {
        String[] split = fqn.split("/");
        if (split.length != 2) {
            throw new IllegalArgumentException("Not a valid component reference string: " + fqn);
        }
        ComponentType<? extends Component> componentType = ComponentTypes.fromName(split[0]);
        if (componentType == null) {
            throw new IllegalArgumentException("Not a valid component type: " + split[0]);
        }

        return of(componentType, ResourceIdentifier.valueOf(split[1]));
    }

    public abstract <K extends Component> ComponentReference<K> widen(Function<T, K> mapper);

    public static class Reference<T extends Component> extends ComponentReference<T> {
        private final ComponentType<T> type;
        private final ResourceIdentifier id;

        public Reference(ComponentType<T> type, ResourceIdentifier id) {
            this.type = type;
            this.id = id;
        }

        @Override
        public Optional<T> getComponent(ComponentAccess catalog) {
            return catalog.getRegistry(this.type).get(this.id);
        }

        @Override
        public Optional<ResourceIdentifier> getId(ComponentAccess catalog) {
            return Optional.of(this.id);
        }

        @Override
        public ComponentType<T> getType() {
            return this.type;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <K extends Component> ComponentReference<K> widen(Function<T, K> mapper) {
            return new Reference<>((ComponentType<K>) this.type, this.id);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Reference<?> reference)) return false;
            return Objects.equals(type, reference.type) && Objects.equals(id, reference.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, id);
        }

        @Override
        public String toString() {
            return String.format("%s/%s", this.type.name, this.id);
        }
    }

    public static class Value<T extends Component> extends ComponentReference<T> {
        private final T value;

        public Value(T value) {
            this.value = value;
        }

        @Override
        public Optional<T> getComponent(ComponentAccess catalog) {
            return Optional.of(this.value);
        }

        @Override
        public Optional<ResourceIdentifier> getId(ComponentAccess catalog) {
            return catalog.getRegistry(this.getType()).getId(this.value);
        }

        @Override
        public ComponentType<T> getType() {
            return ComponentTypes.fromObject(this.value);
        }

        @Override
        public <K extends Component> ComponentReference<K> widen(Function<T, K> mapper) {
            return new Value<>(mapper.apply(this.value));
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Value<?> value1)) return false;
            return Objects.equals(value, value1.value);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        @Override
        public String toString() {
            return String.format("Value[%s]", this.value);
        }
    }

    public static class RefKeyDeserializer extends KeyDeserializer {
        @Override
        public Object deserializeKey(String key, DeserializationContext ctxt) {
            return ComponentReference.valueOf(key);
        }
    }
}
