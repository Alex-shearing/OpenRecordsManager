package com.openrecordsmanager.property;

import org.jspecify.annotations.Nullable;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public interface ObjectPropertyHolder<T extends ObjectPropertyHolder.ObjectPropertyValue<?>> {

    Set<ObjectProperty<?>> getPropertyKeys();

    default Map<String, @Nullable Object> toPropertyMap(boolean forUser) {
        return new PropertyMap(this, forUser);
    }

    <K> @Nullable K getProperty(ObjectProperty<K> property);

    boolean canSetProperty(ObjectProperty<?> property);

    <V> T createProperty(ObjectProperty<V> property, @Nullable V value);

    /**
     * Set a property on the holder, creating it if allowed.
     *
     * @param property the property to set/create
     * @param value    the value to set to
     */
    <K> void setProperty(ObjectProperty<K> property, @Nullable K value);

    default <K> @Nullable K setPropertyUntyped(ObjectProperty<K> property, @Nullable Object value) {
        K newValue = property.getType().cast(value);
        this.setProperty(property, newValue);
        return newValue;
    }

    interface ObjectPropertyValue<T> {
        ObjectProperty<T> getProperty();

        @Nullable T getValue();

        void setValue(@Nullable T value);

        default void setValueUntyped(@Nullable Object value) {
            this.setValue(this.getProperty().getType().cast(value));
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
