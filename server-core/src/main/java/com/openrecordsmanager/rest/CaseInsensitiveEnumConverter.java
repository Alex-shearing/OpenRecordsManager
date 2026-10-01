package com.openrecordsmanager.rest;

import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

/**
 * Binds request {@code String} values to enums by matching {@link Enum#name()} case-insensitively
 * (e.g. {@code create} → {@code CREATE}, {@code record_type} → {@code RECORD_TYPE}).
 */
@Component
public class CaseInsensitiveEnumConverter implements ConditionalGenericConverter {

    @Override
    public @Nullable Set<ConvertiblePair> getConvertibleTypes() {
        return null;
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        Class<?> target = targetType.getType();
        return sourceType.getType() == String.class && target.isEnum();
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public @Nullable Object convert(
            @Nullable Object source,
            TypeDescriptor sourceType,
            TypeDescriptor targetType
    ) {
        if (source instanceof String input) {
            Class<? extends Enum> enumType = (Class<? extends Enum>) targetType.getType();
            return Enum.valueOf(enumType, input.trim().toUpperCase(Locale.ROOT));
        } else if (source == null) {
            return null;
        }

        throw new IllegalArgumentException("could not convert " + source + " to enum");
    }
}
