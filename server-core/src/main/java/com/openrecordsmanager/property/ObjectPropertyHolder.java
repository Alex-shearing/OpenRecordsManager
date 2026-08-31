package com.openrecordsmanager.property;

import com.openrecordsmanager.api.ResourceIdentifier;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public abstract class ObjectPropertyHolder<SELF extends ObjectPropertyHolder<SELF, T>, T extends ObjectPropertyHolder.ObjectPropertyValue<?>> {

    public abstract Set<ObjectProperty<?>> getPropertyKeys();

    protected abstract Map<ObjectProperty<?>, T> getDynamicProperties();

    public final Map<String, @Nullable Object> toPropertyMap(boolean forUser) {
        return new PropertyMap(this, forUser);
    }

    public <K> @Nullable K getProperty(ObjectProperty<K> property) {
        BuiltinPropertyMapper<SELF, ?> builtinMapper = this.getBuiltinPropertyMappers().get(property.getId());

        if (builtinMapper != null) {
            return property.getType().parseValue(builtinMapper.get(this.self()));
        }

        T value = this.getDynamicProperties().get(property);
        if (value == null) {
            return null;
        }
        return property.getType().parseValue(value.getValue());
    }

    public abstract boolean canSetProperty(ObjectProperty<?> property);

    public abstract <V> T createProperty(ObjectProperty<V> property, @Nullable V value);

    protected abstract Map<ResourceIdentifier, BuiltinPropertyMapper<SELF, ?>> getBuiltinPropertyMappers();

    protected abstract SELF self();

    /**
     * Set a property on the holder, creating it if allowed.
     *
     * @param property the property to set/create
     * @param value    the value to set to
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
            T holder = this.getDynamicProperties().get(property);
            if (holder == null) {
                holder = this.createProperty(property, value);
                this.getDynamicProperties().put(property, holder);
            }

            holder.setValueUntyped(value);
        }
    }

    public final <K> @Nullable K setPropertyUntyped(ObjectProperty<K> property, @Nullable Object value) {
        K newValue = property.getType().parseValue(value);
        this.setProperty(property, newValue);
        return newValue;
    }

    public abstract void touchDateModified();

    public interface ObjectPropertyValue<T> {
        ObjectProperty<T> getProperty();

        @Nullable T getValue();

        void setValue(@Nullable T value);

        default void setValueUntyped(@Nullable Object value) {
            this.setValue(this.getProperty().getType().parseValue(value));
        }
    }

    public static class PropertyMap extends AbstractMap<String, Object> {
        private final ObjectPropertyHolder<?, ?> holder;
        private final boolean forUser;

        public PropertyMap(ObjectPropertyHolder<?, ?> holder, boolean forUser) {
            this.holder = holder;
            this.forUser = forUser;
        }

        @Override
        public @Nullable Object get(Object key) {
            if (!(key instanceof String keyString)) {
                return null;
            }

            Optional<ObjectProperty<?>> propKey = this.holder.getPropertyKeys().stream()
                    .filter(property -> !this.forUser || !property.isUserHidden())
                    .filter(property -> property.getId().toString().equals(keyString))
                    .findFirst();

            return propKey
                    .map(this.holder::getProperty)
                    .orElse(null);
        }

        @Override
        @SuppressWarnings("DataFlowIssue") // IDEA things SimpleEntry cannot take a null value
        public Set<Entry<String, Object>> entrySet() {
            return this.holder.getPropertyKeys().stream()
                    .filter(entry -> !this.forUser || !entry.isUserHidden())
                    .map(entry -> new AbstractMap.SimpleEntry<String, Object>(
                            entry.getId().toString(),
                            this.holder.getProperty(entry)
                    ))
                    .collect(Collectors.toSet());
        }
    }
}
