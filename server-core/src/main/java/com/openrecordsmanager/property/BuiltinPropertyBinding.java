package com.openrecordsmanager.property;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.builtin.BuiltinProperties;
import com.openrecordsmanager.api.builtin.BuiltinPropertyIds;
import com.openrecordsmanager.api.template.property.ObjectPropertyTemplate;
import com.openrecordsmanager.api.template.property.PropertyType;
import jakarta.persistence.Column;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Domain access + search metadata for a builtin column-backed property on an {@link ObjectPropertyHolder}.
 * Prefer declaring bindings with {@link BuiltinProperty} on entity fields and {@link #scan(Class)}.
 */
public record BuiltinPropertyBinding<T, P>(
        ResourceIdentifier id,
        String javaAttribute,
        Function<T, @Nullable P> getter,
        BiConsumer<T, @Nullable P> setter,
        PropertyType<?> propertyType,
        boolean jsonStored,
        boolean defaultSearch
) {
    public @Nullable P get(T object) {
        return this.getter.apply(object);
    }

    @SuppressWarnings("unchecked")
    public <K> void set(T object, @Nullable K value) {
        ((BiConsumer<T, @Nullable K>) this.setter).accept(object, value);
    }

    /**
     * Scans {@code entityClass} for {@link BuiltinProperty}-annotated fields (declaration order).
     */
    public static <T> Map<ResourceIdentifier, BuiltinPropertyBinding<T, ?>> scan(Class<T> entityClass) {
        Map<ResourceIdentifier, BuiltinPropertyBinding<T, ?>> indexed = new HashMap<>();

        for (Field field : entityClass.getDeclaredFields()) {
            BuiltinProperty annotation = field.getAnnotation(BuiltinProperty.class);
            if (annotation == null) {
                continue;
            }
            field.setAccessible(true);

            ResourceIdentifier id = BuiltinPropertyIds.id(annotation.value());
            ObjectPropertyTemplate<?> template = BuiltinProperties.BUILTIN_PROPERTIES.get(id);

            if (indexed.containsKey(id)) {
                throw new IllegalArgumentException(
                        "Duplicate @BuiltinProperty id '" + id + "' on " + entityClass.getName()
                );
            }

            indexed.put(id, bindingFromField(entityClass, field, id, template, annotation));
        }

        if (indexed.isEmpty()) {
            throw new IllegalStateException("No @BuiltinProperty fields on " + entityClass.getName());
        }

        return Collections.unmodifiableMap(indexed);
    }

    private static <T> BuiltinPropertyBinding<T, ?> bindingFromField(
            Class<T> entityClass,
            Field field,
            ResourceIdentifier id,
            ObjectPropertyTemplate<?> template,
            BuiltinProperty annotation
    ) {
        String javaAttribute = field.getName();
        boolean required = isRequired(field);
        boolean jsonStored = isJsonStored(field);

        Function<T, @Nullable Object> getter = instance -> {
            try {
                return field.get(instance);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(
                        "Failed to read '" + javaAttribute + "' on " + entityClass.getName(),
                        e
                );
            }
        };

        BiConsumer<T, @Nullable Object> setter = (instance, value) -> {
            if (annotation.readOnly()) {
                throw new IllegalArgumentException(id + " cannot be set explicitly");
            }
            if (required && value == null) {
                throw new IllegalArgumentException(id + " cannot be set to null");
            }

            try {
                field.set(instance, value);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(
                        "Failed to write '" + javaAttribute + "' on " + entityClass.getName(),
                        e
                );
            }
        };

        return new BuiltinPropertyBinding<>(
                id,
                javaAttribute,
                getter,
                setter,
                template.type(),
                jsonStored,
                annotation.defaultSearch()
        );
    }

    private static boolean isRequired(Field field) {
        Column column = field.getAnnotation(Column.class);
        return column != null && !column.nullable();
    }

    private static boolean isJsonStored(Field field) {
        JdbcTypeCode jdbcTypeCode = field.getAnnotation(JdbcTypeCode.class);
        return jdbcTypeCode != null && jdbcTypeCode.value() == SqlTypes.JSON;
    }
}
