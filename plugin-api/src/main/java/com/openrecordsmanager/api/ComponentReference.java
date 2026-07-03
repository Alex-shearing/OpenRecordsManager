package com.openrecordsmanager.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.api.types.ComponentTypes;

import java.util.Objects;
import java.util.function.Function;

public abstract class ComponentReference<T extends Component> {

    abstract public T getComponent(ComponentAccess catalog);

    public abstract ResourceIdentifier getId(ComponentAccess catalog);

    public static <K extends Component> ComponentReference<K> of(K value) {
        return new Value<>(value);
    }

    @JsonCreator
    public static <K extends Component> ComponentReference<K> reference(
            @JsonProperty("type") ComponentType<K> type,
            @JsonProperty("id") ResourceIdentifier id
    ) {
        return new Reference<>(type, id);
    }

    public abstract <K extends Component> ComponentReference<K> map(Function<T, K> mapper);

    public static class Reference<T extends Component> extends ComponentReference<T> {
        private final ComponentType<T> type;
        private final ResourceIdentifier id;

        public Reference(ComponentType<T> type, ResourceIdentifier id) {
            this.type = type;
            this.id = id;
        }

        @Override
        public T getComponent(ComponentAccess catalog) {
            return catalog.getComponent(this.type, this.id).orElse(null);
        }

        @Override
        public ResourceIdentifier getId(ComponentAccess catalog) {
            return this.id;
        }

        @Override
        public <K extends Component> ComponentReference<K> map(Function<T, K> mapper) {
            throw new IllegalArgumentException("not supported");
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
    }

    public static class Value<T extends Component> extends ComponentReference<T> {
        private final T value;

        public Value(T value) {
            this.value = value;
        }

        @Override
        public T getComponent(ComponentAccess catalog) {
            return this.value;
        }

        @Override
        public ResourceIdentifier getId(ComponentAccess catalog) {
            return catalog.getId(ComponentTypes.fromObject(this.value), this.value);
        }

        @Override
        public <K extends Component> ComponentReference<K> map(Function<T, K> mapper) {
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
    }
}
