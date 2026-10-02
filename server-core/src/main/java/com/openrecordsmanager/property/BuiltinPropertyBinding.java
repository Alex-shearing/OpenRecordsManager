package com.openrecordsmanager.property;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.builtin.BuiltinPlugin;
import com.openrecordsmanager.api.builtin.BuiltinProperties;
import com.openrecordsmanager.api.template.property.ObjectPropertyTemplate;
import com.openrecordsmanager.api.template.property.PropertyType;
import jakarta.persistence.Column;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;
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
     * Scans {@code entityClass} and its superclasses up to {@link ObjectPropertyHolder}
     * for {@link BuiltinProperty}-annotated fields (superclass fields first).
     */
    public static <T> Map<ResourceIdentifier, BuiltinPropertyBinding<T, ?>> scan(Class<? extends T> entityClass) {
        Map<ResourceIdentifier, BuiltinPropertyBinding<T, ?>> indexed = new HashMap<>();

        for (Class<?> type : hierarchyClasses(entityClass)) {
            for (Field field : type.getDeclaredFields()) {
                BuiltinProperty annotation = field.getAnnotation(BuiltinProperty.class);
                if (annotation == null) {
                    continue;
                }
                field.setAccessible(true);

                ResourceIdentifier id = new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, annotation.value());
                ObjectPropertyTemplate<?> template = BuiltinProperties.BUILTIN_PROPERTIES.get(annotation.value());

                if (indexed.containsKey(id)) {
                    throw new IllegalArgumentException(
                            "Duplicate @BuiltinProperty id '" + id + "' on " + entityClass.getName()
                    );
                }

                indexed.put(id, bindingFromField(entityClass, field, id, template, annotation));
            }
        }

        if (indexed.isEmpty()) {
            throw new IllegalStateException("No @BuiltinProperty fields on " + entityClass.getName());
        }

        return Collections.unmodifiableMap(indexed);
    }

    private static List<Class<?>> hierarchyClasses(Class<?> entityClass) {
        List<Class<?>> chain = new ArrayList<>();

        for (Class<?> current = entityClass;
             current != null && ObjectPropertyHolder.class.isAssignableFrom(current)
                     && current != ObjectPropertyHolder.class;
             current = current.getSuperclass()
        ) {

            chain.addFirst(current);
        }

        return chain;
    }

    private static <T> BuiltinPropertyBinding<T, ?> bindingFromField(
            Class<?> entityClass,
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
