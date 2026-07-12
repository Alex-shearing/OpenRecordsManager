package com.openrecordsmanager.database.util;

import com.openrecordsmanager.api.ComponentReference;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ComponentReferenceConverter implements AttributeConverter<ComponentReference<?>, String> {
    @Override
    public String convertToDatabaseColumn(ComponentReference<?> resourceIdentifier) {
        return resourceIdentifier.toString();
    }

    @Override
    public ComponentReference<?> convertToEntityAttribute(String s) {
        return ComponentReference.valueOf(s);
    }
}
