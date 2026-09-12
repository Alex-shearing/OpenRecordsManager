package com.openrecordsmanager.rest.swagger;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.Schema;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Iterator;

/**
 * Maps Jackson 3 {@link JsonNode} to a free-form OpenAPI schema.
 * Without this, springdoc does not represent JsonNode correctly.
 */
public final class JsonNodeModelConverter implements ModelConverter {

    static Schema<?> freeFormJsonSchema() {
        return new Schema<>()
                .description("Arbitrary JSON value (string, number, boolean, object, array, or null)");
    }

    @Override
    @Nullable
    public Schema<?> resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        if (isJsonNode(type.getType())) {
            return freeFormJsonSchema();
        }
        return chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
    }

    private static boolean isJsonNode(Type type) {
        Class<?> raw = rawClass(type);
        return raw != null && JsonNode.class.isAssignableFrom(raw);
    }

    @Nullable
    private static Class<?> rawClass(Type type) {
        if (type instanceof Class<?> clazz) {
            return clazz;
        }
        if (type instanceof ParameterizedType parameterized) {
            Type raw = parameterized.getRawType();
            if (raw instanceof Class<?> clazz) {
                return clazz;
            }
        }
        return null;
    }
}
