package com.openrecordsmanager.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.openrecordsmanager.api.types.ComponentType;
import com.openrecordsmanager.api.types.ComponentTypes;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.KeyDeserializer;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;

import java.util.Objects;
import java.util.function.Function;

public abstract class ComponentReference<T extends Component> {

    abstract public T getComponent(ComponentAccess catalog);

    public abstract ResourceIdentifier getId(ComponentAccess catalog);

    public static <K extends Component> ComponentReference<K> of(K value) {
        return new Value<>(value);
    }

    public static <K extends Component> ComponentReference<K> reference(ComponentType<K> type, ResourceIdentifier id) {
        return new Reference<>(type, id);
    }

    public static ComponentReference<Component> reference(String fqn) {
        String[] split = fqn.split("/");
        if (split.length != 2) {
            throw new IllegalArgumentException("Not a valid component reference string: " + fqn);
        }

        return reference(ComponentTypes.fromName(split[0]), ResourceIdentifier.valueOf(split[1]));
    }

    @JsonCreator
    protected static ComponentReference<Component> fromJsonNode(JsonNode node) {
        return switch (node) {
            case StringNode n -> reference(n.asString());
            case ObjectNode n ->
                    reference(ComponentTypes.fromName(n.get("type").asString()), ResourceIdentifier.valueOf(n.get("id").asString()));
            default -> throw new IllegalStateException("Unable to parse input as a component reference: " + node);
        };
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
        public T getComponent(ComponentAccess catalog) {
            return catalog.getComponent(this.type, this.id).orElse(null);
        }

        @Override
        public ResourceIdentifier getId(ComponentAccess catalog) {
            return this.id;
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
    }

    public static class ComponentReferenceKeyDeserializer extends KeyDeserializer {
        @Override
        public Object deserializeKey(String key, DeserializationContext ctxt) {
            return ComponentReference.reference(key);
        }
    }
}
