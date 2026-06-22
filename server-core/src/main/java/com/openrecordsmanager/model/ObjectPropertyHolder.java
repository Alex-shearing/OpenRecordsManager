package com.openrecordsmanager.model;

import com.openrecordsmanager.resources.ResourceIdentifier;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public interface ObjectPropertyHolder<T extends ObjectPropertyHolder.ObjectPropertyValue<?>> {

    Set<T> getProperties();

    default Map<String, Object> toPropertyMap() {
        return this.getProperties().stream().map(el -> Map.entry(el.getProperty().id.toString(), el.getValue())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    default Object getProperty(ResourceIdentifier id) {
        Optional<T> property = this.getProperties().stream().filter(recordPropertyValue -> Objects.equals(recordPropertyValue.getProperty().id, id)).findFirst();
        return property.map(userPropertyValue -> userPropertyValue.getValue()).orElse(null);
    }

    @SuppressWarnings("unchecked")
    default <K> K getProperty(ObjectProperty<K> userProperty) {
        return (K) this.getProperty(userProperty.id);
    }

    interface ObjectPropertyValue<T> {
        ObjectProperty<T> getProperty();

        T getValue();
    }

}
