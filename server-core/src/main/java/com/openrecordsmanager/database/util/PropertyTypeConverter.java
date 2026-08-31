package com.openrecordsmanager.database.util;

import com.openrecordsmanager.api.template.property.PropertyType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PropertyTypeConverter implements AttributeConverter<PropertyType<?>, String> {
    @Override
    public String convertToDatabaseColumn(PropertyType<?> propertyType) {
        return propertyType.getName();
    }

    @Override
    public PropertyType<?> convertToEntityAttribute(String s) {
        return PropertyType.TYPES.get(s);
    }
}
