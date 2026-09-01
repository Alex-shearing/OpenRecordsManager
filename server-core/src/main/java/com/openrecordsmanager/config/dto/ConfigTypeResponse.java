package com.openrecordsmanager.config.dto;

import com.openrecordsmanager.api.config.ConfigType;
import com.openrecordsmanager.config.dto.schema.ConfigTypeResponseSchema;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

@Schema(
        oneOf = {
                ConfigTypeResponseSchema.StringType.class,
                ConfigTypeResponseSchema.BooleanType.class,
                ConfigTypeResponseSchema.NumberType.class,
                ConfigTypeResponseSchema.DecimalType.class,
                ConfigTypeResponseSchema.UuidType.class,
                ConfigTypeResponseSchema.StringListType.class,
                ConfigTypeResponseSchema.IntListType.class,
                ConfigTypeResponseSchema.ObjectType.class
        },
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "string", schema = ConfigTypeResponseSchema.StringType.class),
                @DiscriminatorMapping(value = "boolean", schema = ConfigTypeResponseSchema.BooleanType.class),
                @DiscriminatorMapping(value = "number", schema = ConfigTypeResponseSchema.NumberType.class),
                @DiscriminatorMapping(value = "decimal", schema = ConfigTypeResponseSchema.DecimalType.class),
                @DiscriminatorMapping(value = "uuid", schema = ConfigTypeResponseSchema.UuidType.class),
                @DiscriminatorMapping(value = "string_list", schema = ConfigTypeResponseSchema.StringListType.class),
                @DiscriminatorMapping(value = "int_list", schema = ConfigTypeResponseSchema.IntListType.class),
                @DiscriminatorMapping(value = "object", schema = ConfigTypeResponseSchema.ObjectType.class)
        }
)
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
