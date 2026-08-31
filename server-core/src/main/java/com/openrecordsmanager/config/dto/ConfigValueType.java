package com.openrecordsmanager.config.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.openrecordsmanager.api.template.property.PropertyType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum ConfigValueType {
    STRING("string"),
    BOOLEAN("boolean"),
    NUMBER("number"),
    DECIMAL("decimal"),
    UUID("uuid"),
    STRING_LIST("string_list"),
    INT_LIST("int_list"),
    OBJECT("object");

    private static final Map<String, ConfigValueType> BY_NAME = Stream.of(values())
            .collect(Collectors.toMap(ConfigValueType::getName, type -> type));

    private final String name;

    ConfigValueType(String name) {
        this.name = name;
    }

    @JsonValue
    @Schema(description = "Property type name")
    public String getName() {
        return this.name;
    }

    @JsonCreator
    public static ConfigValueType fromName(String name) {
        ConfigValueType type = BY_NAME.get(name);
        if (type == null) {
            throw new IllegalArgumentException("Unknown config value type: " + name);
        }
        return type;
    }

    public static ConfigValueType fromPropertyType(PropertyType<?> propertyType) {
        return fromName(propertyType.getName());
    }
}
