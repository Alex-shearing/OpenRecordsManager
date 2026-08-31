package com.openrecordsmanager.config.dto;

import com.openrecordsmanager.api.config.ConfigType;
import com.openrecordsmanager.api.config.ConfigValueType;
import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;

public record ConfigTypeResponse(
        @NotBlank String key,
        @NotBlank String name,
        @Nullable Object currentValue,
        @NotBlank String description,
        @Nullable Object defaultValue,
        @NotBlank ConfigTypeResponse.ConfigValType type
) {

    public static <T> ConfigTypeResponse from(ConfigType<T> ob, T val) {
        return new ConfigTypeResponse(
                ob.key(),
                ob.name(),
                val,
                ob.description(),
                ob.defaultValue(),
                ConfigValType.from(ob.type())
        );
    }

    public enum ConfigValType {
        STRING,
        INT,
        DOUBLE,
        BOOL,
        UUID,
        STRING_LIST,
        INT_LIST,
        UNKNOWN;

        public static ConfigValType from(ConfigValueType<?> type) {
            if (type == ConfigValueType.STRING) {
                return STRING;
            }
            if (type == ConfigValueType.INT) {
                return INT;
            }
            if (type == ConfigValueType.DOUBLE) {
                return DOUBLE;
            }
            if (type == ConfigValueType.BOOL) {
                return BOOL;
            }
            if (type == ConfigValueType.UUID) {
                return UUID;
            }
            if (type == ConfigValueType.STRING_LIST) {
                return STRING_LIST;
            }
            if (type == ConfigValueType.INT_LIST) {
                return INT_LIST;
            }
            return UNKNOWN;
        }
    }
}
