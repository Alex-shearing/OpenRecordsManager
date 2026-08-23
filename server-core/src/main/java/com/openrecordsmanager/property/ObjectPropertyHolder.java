package com.openrecordsmanager.property;

import org.jspecify.annotations.Nullable;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public interface ObjectPropertyHolder<T extends ObjectPropertyHolder.ObjectPropertyValue<?>> {

    Map<ObjectProperty<?>, T> getProperties();

    default Map<String, @Nullable Object> toPropertyMap(boolean forUser) {
        return new PropertyMap(this, forUser);
    }

    default <K> @Nullable K getProperty(ObjectProperty<K> property) {
        ObjectPropertyValue<?> value = this.getProperties().get(property);
        if (value == null) {
            return null;
        }
        return property.getType().cast(value.getValue());
    }

    boolean canSetProperty(ObjectProperty<?> property);

    <V> T createProperty(ObjectProperty<V> property, @Nullable V value);

    /**
     * Set a property on the holder, creating it if allowed.
     *
     * @param property the property to set/create
     * @param value    the value to set to
     */
    default <K> void setProperty(ObjectProperty<K> property, @Nullable K value) {
        T holder = this.getProperties().get(property);
        if (holder == null) {
            if (!this.canSetProperty(property)) {
                throw new IllegalArgumentException("Property " + property + " does not exist on object");
            }

            holder = this.createProperty(property, value);
            this.getProperties().put(property, holder);
        }

        holder.setValueRaw(value);
    }

    interface ObjectPropertyValue<T> {
        ObjectProperty<T> getProperty();

        @Nullable T getValue();

        void setValue(@Nullable T value);

        default void setValueRaw(@Nullable Object value) {
            this.setValue(getProperty().getType().cast(value));
        }
    }

    class PropertyMap extends AbstractMap<String, Object> {
        private final ObjectPropertyHolder<?> holder;
        private final boolean forUser;

        public PropertyMap(ObjectPropertyHolder<?> holder, boolean forUser) {
            this.holder = holder;
            this.forUser = forUser;
        }

        @Override
        public @Nullable Object get(Object key) {
            if (!(key instanceof String keyString)) {
                return null;
            }

            Optional<ObjectProperty<?>> propKey = this.holder.getProperties().keySet().stream()
                    .filter(property -> !this.forUser || !property.isUserHidden())
                    .filter(property -> property.getId().toString().equals(keyString))
                    .findFirst();

            return propKey
                    .map(this.holder::getProperty)
                    .orElse(null);
        }

        @Override
        public Set<Entry<String, Object>> entrySet() {
            return this.holder.getProperties().entrySet().stream()
                    .filter(entry -> !this.forUser || !entry.getKey().isUserHidden())
                    .map(entry -> new AbstractMap.SimpleEntry<String, Object>(
                            entry.getKey().getId().toString(),
                            entry.getValue().getValue()
                    ))
                    .collect(Collectors.toSet());
        }
    }
}
