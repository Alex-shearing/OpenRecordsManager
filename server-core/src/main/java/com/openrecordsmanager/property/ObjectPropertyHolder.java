package com.openrecordsmanager.property;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.errors.InputValidationException;
import com.openrecordsmanager.api.template.property.PropertyType;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.NullNode;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Typed property bag for users/records.
 * Domain access uses {@code T}; wire/storage uses {@link JsonNode}.
 */
public abstract class ObjectPropertyHolder<SELF extends ObjectPropertyHolder<SELF, V>, V extends ObjectPropertyHolder.ObjectPropertyValue> {

    private static volatile @Nullable PropertyValueCodec codec;

    static void installCodec(PropertyValueCodec propertyValueCodec) {
        codec = propertyValueCodec;
    }

    static void uninstallCodec(PropertyValueCodec propertyValueCodec) {
        if (codec == propertyValueCodec) {
            codec = null;
        }
    }

    public abstract Set<ObjectProperty<?>> getPropertyKeys();

    protected abstract Map<ObjectProperty<?>, V> getDynamicProperties();

    /**
     * API wire map (list values as id strings).
     */
    public final Map<String, @Nullable JsonNode> toWireMap() {
        return new WirePropertyMap(this);
    }

    /**
     * Domain map for CEL (heterogeneous {@code T} values; erasure only at this DYN boundary).
     */
    public final Map<String, @Nullable Object> toDomainMap() {
        return new DomainPropertyMap(this);
    }

    public <K> @Nullable K getProperty(ObjectProperty<K> property) {
        BuiltinPropertyMapper<SELF, ?> builtinMapper = this.getBuiltinPropertyMappers().get(property.getId());

        if (builtinMapper != null) {
            return property.getType().parseValue(builtinMapper.get(this.self()));
        }

        V stored = this.getDynamicProperties().get(property);
        if (stored == null) {
            return null;
        }

        JsonNode raw = stored.getStoredValue();
        PropertyValueCodec activeCodec = codec;
        if (activeCodec != null) {
            @SuppressWarnings("unchecked")
            K hydrated = (K) activeCodec.hydrate(property, raw);
            return hydrated;
        }
        return property.getType().parse(raw);
    }

    public abstract boolean canSetProperty(ObjectProperty<?> property);

    public abstract V createProperty(ObjectProperty<?> property, @Nullable JsonNode storedValue);

    protected abstract Map<ResourceIdentifier, BuiltinPropertyMapper<SELF, ?>> getBuiltinPropertyMappers();

    protected abstract SELF self();

    /**
     * Set a typed domain property. Persists as {@link JsonNode} via the codec when installed.
     */
    public final <K> void setProperty(ObjectProperty<K> property, @Nullable K value) {
        if (!this.canSetProperty(property)) {
            throw new IllegalArgumentException("Property " + property + " does not exist on object");
        }

        BuiltinPropertyMapper<SELF, ?> mapper = this.getBuiltinPropertyMappers().get(property.getId());
        if (mapper != null) {
            K oldValue = this.getProperty(property);
            if (!Objects.equals(oldValue, value)) {
                mapper.set(this.self(), value);
                this.touchDateModified();
            }
            return;
        }

        K oldValue = this.getProperty(property);
        if (!Objects.equals(oldValue, value)) {
            this.touchDateModified();

            JsonNode stored = NullNode.getInstance();
            PropertyValueCodec activeCodec = codec;
            if (activeCodec != null) {
                stored = activeCodec.toStored(property, value);
            } else if (value != null) {
                stored = PropertyType.toTree(value);
            }

            V holder = this.getDynamicProperties().get(property);
            if (holder == null) {
                holder = this.createProperty(property, stored);
                this.getDynamicProperties().put(property, holder);
            } else {
                holder.setStoredValue(stored);
            }
        }
    }

    /**
     * Set from wire/storage {@link JsonNode}: parse once to domain, then store as JsonNode.
     */
    public final <K> @Nullable K setPropertyFromJson(ObjectProperty<K> property, @Nullable JsonNode value) {
        PropertyValueCodec activeCodec = codec;
        K newValue;
        if (activeCodec != null) {
            newValue = activeCodec.parse(property, value);
        } else {
            newValue = property.getType().parse(value);
            if (newValue == null && value != null && !value.isNull()) {
                throw new InputValidationException(Map.of(
                        property.getId().toString(),
                        "unable to parse value as " + property.getType().getName()
                ));
            }
        }

        this.setProperty(property, newValue);
        return newValue;
    }

    public abstract void touchDateModified();

    public interface ObjectPropertyValue {
        ObjectProperty<?> getProperty();

        @Nullable JsonNode getStoredValue();

        void setStoredValue(@Nullable JsonNode value);
    }

    private static final class WirePropertyMap extends AbstractMap<String, JsonNode> {
        private final ObjectPropertyHolder<?, ?> holder;

        private WirePropertyMap(ObjectPropertyHolder<?, ?> holder) {
            this.holder = holder;
        }

        @Override
        public @Nullable JsonNode get(Object key) {
            if (!(key instanceof String keyString)) {
                return null;
            }
            return findProperty(keyString)
                    .map(property -> encode(property, this.holder.getProperty(property)))
                    .orElse(null);
        }

        @Override
        public Set<Entry<String, JsonNode>> entrySet() {
            return this.holder.getPropertyKeys().stream()
                    .filter(entry -> !entry.isUserHidden())
                    .map(entry -> new AbstractMap.SimpleEntry<>(
                            entry.getId().toString(),
                            encode(entry, this.holder.getProperty(entry))
                    ))
                    .collect(Collectors.toSet());
        }

        private Optional<ObjectProperty<?>> findProperty(String keyString) {
            return this.holder.getPropertyKeys().stream()
                    .filter(property -> !property.isUserHidden())
                    .filter(property -> property.getId().toString().equals(keyString))
                    .findFirst();
        }

        private static JsonNode encode(ObjectProperty<?> property, @Nullable Object domainValue) {
            PropertyValueCodec activeCodec = codec;
            if (activeCodec != null) {
                return activeCodec.encode(property, domainValue);
            }
            if (domainValue == null) {
                return NullNode.getInstance();
            }
            return PropertyType.toTree(domainValue);
        }
    }

    private static final class DomainPropertyMap extends AbstractMap<String, Object> {
        private final ObjectPropertyHolder<?, ?> holder;

        private DomainPropertyMap(ObjectPropertyHolder<?, ?> holder) {
            this.holder = holder;
        }

        @Override
        public @Nullable Object get(Object key) {
            if (!(key instanceof String keyString)) {
                return null;
            }
            return this.holder.getPropertyKeys().stream()
                    .filter(property -> property.getId().toString().equals(keyString))
                    .findFirst()
                    .map(this.holder::getProperty)
                    .orElse(null);
        }

        @Override
        @SuppressWarnings("DataFlowIssue")
        public Set<Entry<String, Object>> entrySet() {
            return this.holder.getPropertyKeys().stream()
                    .map(entry -> new AbstractMap.SimpleEntry<String, Object>(
                            entry.getId().toString(),
                            this.holder.getProperty(entry)
                    ))
                    .collect(Collectors.toSet());
        }
    }
}
