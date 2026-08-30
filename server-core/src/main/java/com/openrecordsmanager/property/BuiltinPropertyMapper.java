package com.openrecordsmanager.property;

import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Function;

public record BuiltinPropertyMapper<T, P>(
        Function<T, @Nullable P> getter,
        BiConsumer<T, @Nullable P> setter
) {
    public static <T, P> BuiltinPropertyMapper<T, P> of(
            Function<T, @Nullable P> getter,
            BiConsumer<T, @Nullable P> setter
    ) {
        return new BuiltinPropertyMapper<>(getter, setter);
    }

    public @Nullable P get(T object) {
        return this.getter.apply(object);
    }

    @SuppressWarnings("unchecked")
    public <K> void set(T object, @Nullable K value) {
        ((BiConsumer<T, @Nullable K>) this.setter).accept(object, value);
    }
}
