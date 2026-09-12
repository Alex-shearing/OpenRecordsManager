package com.openrecordsmanager.database;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.AbstractJsonFormatMapper;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * Hibernate JSON format mapper for Jackson 3 ({@code tools.jackson}).
 * Spring Boot 4 / Hibernate 7.2 still auto-selects Jackson 2 when both are on the classpath;
 * this mapper is required for {@link tools.jackson.databind.JsonNode} columns.
 */
public final class ToolsJacksonJsonFormatMapper extends AbstractJsonFormatMapper {

    private final JsonMapper jsonMapper;

    public ToolsJacksonJsonFormatMapper() {
        this(JsonMapper.builder().build());
    }

    public ToolsJacksonJsonFormatMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public <T> void writeToTarget(T value, JavaType<T> javaType, Object target, WrapperOptions options) throws IOException {
        this.jsonMapper.writerFor(this.jsonMapper.constructType(javaType.getJavaType()))
                .writeValue((JsonGenerator) target, value);
    }

    @Override
    public <T> T readFromSource(JavaType<T> javaType, Object source, WrapperOptions options) throws IOException {
        return this.jsonMapper.readValue((JsonParser) source, this.jsonMapper.constructType(javaType.getJavaType()));
    }

    @Override
    public boolean supportsSourceType(Class<?> sourceType) {
        return JsonParser.class.isAssignableFrom(sourceType);
    }

    @Override
    public boolean supportsTargetType(Class<?> targetType) {
        return JsonGenerator.class.isAssignableFrom(targetType);
    }

    @Override
    protected <T> T fromString(CharSequence charSequence, Type type) {
        return this.jsonMapper.readValue(charSequence.toString(), this.jsonMapper.constructType(type));
    }

    @Override
    protected <T> String toString(T value, Type type) {
        return this.jsonMapper.writerFor(this.jsonMapper.constructType(type)).writeValueAsString(value);
    }
}
