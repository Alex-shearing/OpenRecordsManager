package com.openrecordsmanager.model.util;

import com.openrecordsmanager.resources.ResourceIdentifier;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ResourceIdentifierConverter implements AttributeConverter<ResourceIdentifier, String> {
    @Override
    public String convertToDatabaseColumn(ResourceIdentifier resourceIdentifier) {
        return resourceIdentifier.toString();
    }

    @Override
    public ResourceIdentifier convertToEntityAttribute(String s) {
        return ResourceIdentifier.valueOf(s);
    }
}
