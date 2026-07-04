package com.openrecordsmanager.api.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.openrecordsmanager.api.Component;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class ComponentType<T extends Component> {

    public final String name;
    private final Class<T> componentClass;

    public ComponentType(String name, Class<T> componentClass) {
        this.name = name;
        this.componentClass = componentClass;
    }

    public <K extends Component> boolean is(K object) {
        return this.componentClass.isInstance(object);
    }

    @Override
    public boolean equals(Object o) {
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
    private static @Nullable ComponentType<Component> fromString(String name) {
        return ComponentTypes.fromName(name);
    }
}
