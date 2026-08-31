package com.openrecordsmanager.config.dto;

import com.openrecordsmanager.api.config.ConfigType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

public record ConfigTypeResponse(
        @NotBlank String key,
        @NotBlank String name,
        @Nullable Object currentValue,
        @NotBlank String description,
        @Nullable Object defaultValue,
        @NotNull ConfigValueType type
) {

    public static <T> ConfigTypeResponse from(ConfigType<T> ob, T val) {
        return new ConfigTypeResponse(
                ob.key(),
                ob.name(),
                val,
                ob.description(),
                ob.defaultValue(),
                ConfigValueType.fromPropertyType(ob.type())
        );
    }
}
