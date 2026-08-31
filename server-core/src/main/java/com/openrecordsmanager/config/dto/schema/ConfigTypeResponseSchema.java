package com.openrecordsmanager.config.dto.schema;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * OpenAPI oneOf schemas for {@link com.openrecordsmanager.config.dto.ConfigTypeResponse}.
 * Runtime responses use a single record shape; these types document type-specific value shapes.
 */
public final class ConfigTypeResponseSchema {

    private ConfigTypeResponseSchema() {
    }

    @Schema(
            description = "String configuration value",
            requiredProperties = {"key", "name", "description", "type"}
    )
    public record StringType(
            String key,
            String name,
            String description,
            @Schema(type = "string", allowableValues = {"string"}) String type,
            String currentValue,
            String defaultValue
    ) {
    }

    @Schema(
            description = "Boolean configuration value",
            requiredProperties = {"key", "name", "description", "type"}
    )
    public record BooleanType(
            String key,
            String name,
            String description,
            @Schema(type = "string", allowableValues = {"boolean"}) String type,
            Boolean currentValue,
            Boolean defaultValue
    ) {
    }

    @Schema(
            description = "Integer number configuration value",
            requiredProperties = {"key", "name", "description", "type"}
    )
    public record NumberType(
            String key,
            String name,
            String description,
            @Schema(type = "string", allowableValues = {"number"}) String type,
            Long currentValue,
            Long defaultValue
    ) {
    }

    @Schema(
            description = "Decimal configuration value",
            requiredProperties = {"key", "name", "description", "type"}
    )
    public record DecimalType(
            String key,
            String name,
            String description,
            @Schema(type = "string", allowableValues = {"decimal"}) String type,
            Double currentValue,
            Double defaultValue
    ) {
    }

    @Schema(
            description = "UUID configuration value",
            requiredProperties = {"key", "name", "description", "type"}
    )
    public record UuidType(
            String key,
            String name,
            String description,
            @Schema(type = "string", allowableValues = {"uuid"}) String type,
            String currentValue,
            String defaultValue
    ) {
    }

    @Schema(
            description = "String list configuration value",
            requiredProperties = {"key", "name", "description", "type"}
    )
    public record StringListType(
            String key,
            String name,
            String description,
            @Schema(type = "string", allowableValues = {"string_list"}) String type,
            List<String> currentValue,
            List<String> defaultValue
    ) {
    }

    @Schema(
            description = "Integer list configuration value",
            requiredProperties = {"key", "name", "description", "type"}
    )
    public record IntListType(
            String key,
            String name,
            String description,
            @Schema(type = "string", allowableValues = {"int_list"}) String type,
            List<Integer> currentValue,
            List<Integer> defaultValue
    ) {
    }

    @Schema(
            description = "Opaque object configuration value",
            requiredProperties = {"key", "name", "description", "type"}
    )
    public record ObjectType(
            String key,
            String name,
            String description,
            @Schema(type = "string", allowableValues = {"object"}) String type,
            Object currentValue,
            Object defaultValue
    ) {
    }
}
