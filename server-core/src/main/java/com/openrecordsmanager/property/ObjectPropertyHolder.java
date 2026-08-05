package com.openrecordsmanager.property;

import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.stream.Collectors;

public interface ObjectPropertyHolder<T extends ObjectPropertyHolder.ObjectPropertyValue<?>> {

    Map<ObjectProperty<?>, T> getProperties();

    default Map<String, Object> toPropertyMap() {
        return this.getProperties().entrySet().stream()
                .collect(Collectors.toMap(
                        el -> el.getKey().id.toString(),
                        el -> el.getValue().getValue()
                ));
    }

    default <K> @Nullable K getProperty(ObjectProperty<K> property) {
        return property.type.cast(this.getProperties().get(property).getValue());
    }

    boolean canSetProperty(ObjectProperty<?> property);

    <V> T createProperty(ObjectProperty<V> property, V value);

    /**
     * Set a property on the holder, creating it if allowed.
     *
     * @param property the property to set/create
     * @param value    the value to set to
     */
    default <K> void setProperty(ObjectProperty<K> property, K value) {
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

        default void setValueRaw(Object value) {
            this.setValue(getProperty().type.cast(value));
        }
    }
}
