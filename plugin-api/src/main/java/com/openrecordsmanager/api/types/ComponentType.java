package com.openrecordsmanager.api.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.openrecordsmanager.api.Component;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public class ComponentType<T extends Component> {

    public final String name;
    private final Class<T> componentClass;

    public ComponentType(String name, Class<T> componentClass) {
        this.name = name;
        this.componentClass = componentClass;
    }

    public <K extends Component> Optional<T> get(K object) {
        if (this.componentClass.isInstance(object)) {
            return Optional.of(this.componentClass.cast(object));
        }
        return Optional.empty();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ComponentType<?> that = (ComponentType<?>) o;
        return Objects.equals(componentClass, that.componentClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.componentClass.getSimpleName(), this.name);
    }

    @Override
    public String toString() {
        return this.name;
    }

    @SuppressWarnings("unchecked")
    public static <A extends Component, K extends A> ComponentType<K> of(String name, Class<A> componentClass) {
        return (ComponentType<K>) new ComponentType<>(name, componentClass);
    }

    @JsonCreator
    @SuppressWarnings("unused")
    private static @Nullable ComponentType<?> fromString(String name) {
        return ComponentTypes.fromName(name);
    }
}
