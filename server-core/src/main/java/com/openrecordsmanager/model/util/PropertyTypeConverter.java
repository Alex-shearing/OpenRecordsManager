package com.openrecordsmanager.model.util;

import com.openrecordsmanager.property.PropertyType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PropertyTypeConverter implements AttributeConverter<PropertyType<?>, String> {
    @Override
    public String convertToDatabaseColumn(PropertyType<?> attribute) {
        return attribute.name;
    }

    @Override
    public PropertyType<?> convertToEntityAttribute(String dbData) {
        return PropertyType.TYPES.get(dbData);
    }
}
