package com.openrecordsmanager.property;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.builtin.BuiltinPlugin;
import com.openrecordsmanager.api.builtin.BuiltinPropertyIds;
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

    public abstract Set<ObjectProperty<?>> getPropertyKeys();

    protected abstract Map<ObjectProperty<?>, V> getDynamicProperties();

    /**
     * API wire map (list values as id strings).
     */
    public final Map<String, JsonNode> toWireMap() {
        return new WirePropertyMap(this);
    }

    /**
     * Domain map for CEL (heterogeneous {@code T} values; erasure only at this DYN boundary).
     */
    public final Map<String, @Nullable Object> toDomainMap() {
        return new DomainPropertyMap(this);
    }

    final boolean hasBuiltin(ResourceIdentifier id) {
        return this.getBuiltinPropertyBindings().containsKey(id);
    }

    final @Nullable Object readBuiltin(ResourceIdentifier id) {
        BuiltinPropertyBinding<SELF, ?> binding = this.getBuiltinPropertyBindings().get(id);
        return binding == null ? null : binding.get(this.self());
    }

    public <K> @Nullable K getProperty(ObjectProperty<K> property) {
        BuiltinPropertyBinding<SELF, ?> binding = this.getBuiltinPropertyBindings().get(property.getId());

        if (binding != null) {
            @SuppressWarnings("unchecked")
            K value = (K) binding.get(this.self());
            return value;
        }

        V stored = this.getDynamicProperties().get(property);
        if (stored == null) {
            return null;
        }

        JsonNode raw = stored.getStoredValue();
        @SuppressWarnings("unchecked")
        K hydrated = (K) PropertyValueCodec.requireInstalled().hydrate(property, raw);
        return hydrated;
    }

    public abstract boolean canSetProperty(ObjectProperty<?> property);

    public abstract V createProperty(ObjectProperty<?> property, JsonNode storedValue);

    protected abstract Map<ResourceIdentifier, BuiltinPropertyBinding<SELF, ?>> getBuiltinPropertyBindings();

    protected abstract SELF self();

    /**
     * Set a typed domain property. Persists as {@link JsonNode} via
     * {@link PropertyValueCodec#toStored}. Domain {@code null} removes the dynamic property row.
     */
    public final <K> void setProperty(ObjectProperty<K> property, @Nullable K value) {
        if (!this.canSetProperty(property)) {
            throw new IllegalArgumentException("Property " + property + " does not exist on object");
        }

        BuiltinPropertyBinding<SELF, ?> binding = this.getBuiltinPropertyBindings().get(property.getId());
        if (binding != null) {
            K oldValue = this.getProperty(property);
            if (!Objects.equals(oldValue, value)) {
                binding.set(this.self(), value);
                this.touchDateModified();
            }
            return;
        }

        K oldValue = this.getProperty(property);
        if (!Objects.equals(oldValue, value)) {
            this.touchDateModified();

            JsonNode stored = PropertyValueCodec.requireInstalled().toStored(property, value);

            if (stored.isNull()) {
                this.getDynamicProperties().remove(property);
                return;
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
        K newValue = PropertyValueCodec.requireInstalled().parse(property, value);
        this.setProperty(property, newValue);
        return newValue;
    }

    public abstract void touchDateModified();

    public interface ObjectPropertyValue {
        ObjectProperty<?> getProperty();

        JsonNode getStoredValue();

        void setStoredValue(JsonNode value);
    }

    private static final class WirePropertyMap extends AbstractMap<String, JsonNode> {
        private static final List<ResourceIdentifier> ALWAYS_ON_WIRE = List.of(
                new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinPropertyIds.ID),
                new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinPropertyIds.DATE_CREATED),
                new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinPropertyIds.DATE_MODIFIED)
        );

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
                    .orElseGet(() -> encodeAlwaysOnBuiltin(keyString));
        }

        @Override
        public Set<Entry<String, JsonNode>> entrySet() {
            Set<Entry<String, JsonNode>> entries = this.holder.getPropertyKeys().stream()
                    .filter(entry -> !entry.isUserHidden())
                    .map(entry -> new AbstractMap.SimpleEntry<>(
                            entry.getId().toString(),
                            encode(entry, this.holder.getProperty(entry))
                    ))
                    .collect(Collectors.toSet());

            for (ResourceIdentifier id : ALWAYS_ON_WIRE) {
                JsonNode value = encodeAlwaysOnBuiltin(id.toString());
                if (value != null && entries.stream().noneMatch(entry -> id.toString().equals(entry.getKey()))) {
                    entries.add(new AbstractMap.SimpleEntry<>(id.toString(), value));
                }
            }
            return entries;
        }

        private Optional<ObjectProperty<?>> findProperty(String keyString) {
            return this.holder.getPropertyKeys().stream()
                    .filter(property -> !property.isUserHidden())
                    .filter(property -> property.getId().toString().equals(keyString))
                    .findFirst();
        }

        private @Nullable JsonNode encodeAlwaysOnBuiltin(String keyString) {
            for (ResourceIdentifier id : ALWAYS_ON_WIRE) {
                if (!id.toString().equals(keyString) || !this.holder.hasBuiltin(id)) {
                    continue;
                }
                Object value = this.holder.readBuiltin(id);
                return value == null ? NullNode.getInstance() : PropertyType.toTree(value);
            }
            return null;
        }

        private static JsonNode encode(ObjectProperty<?> property, @Nullable Object domainValue) {
            return PropertyValueCodec.requireInstalled().encode(property, domainValue);
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
