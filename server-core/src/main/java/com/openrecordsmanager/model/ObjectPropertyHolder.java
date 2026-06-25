package com.openrecordsmanager.model;

import com.openrecordsmanager.resources.ResourceIdentifier;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public interface ObjectPropertyHolder<T extends ObjectPropertyHolder.ObjectPropertyValue<?>> {

    Map<ObjectProperty<?>, T> getProperties();

    default Map<String, Object> toPropertyMap() {
        return this.getProperties().entrySet().stream().map(el -> Map.entry(el.getKey().id.toString(), el.getValue().getValue())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    default Object getProperty(ResourceIdentifier id) {
        Optional<ObjectProperty<?>> property = this.getProperties().keySet().stream().filter(prop -> Objects.equals(prop.id, id)).findFirst();
        return property.map(this::getProperty).orElse(null);
    }

    @SuppressWarnings("unchecked")
    default <K> K getProperty(ObjectProperty<K> property) {
        return (K) this.getProperties().get(property).getValue();
    }

    boolean canSetProperty(ObjectProperty<?> property);

    <V> T createProperty(ObjectProperty<V> property, V value);

    default <K> void setProperty(ObjectProperty<K> property, K value) {
        T holder = this.getProperties().get(property);
        if (holder == null) {
            if (!this.canSetProperty(property)) {
                throw new IllegalArgumentException("Property " + property + " does not exist on object");
            }

            holder = this.createProperty(property, value);
            this.getProperties().put(property, holder);
        }

        holder.setValueUnchecked(value);
    }

    interface ObjectPropertyValue<T> {
        ObjectProperty<T> getProperty();

        T getValue();

        void setValue(T value);

        @SuppressWarnings("unchecked")
        default void setValueUnchecked(Object value) {
            this.setValue((T) value);
        }
    }
}
