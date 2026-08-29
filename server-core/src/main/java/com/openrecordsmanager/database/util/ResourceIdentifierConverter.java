package com.openrecordsmanager.database.util;

import com.openrecordsmanager.api.ResourceIdentifier;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.jspecify.annotations.Nullable;

@Converter(autoApply = true)
public class ResourceIdentifierConverter implements AttributeConverter<ResourceIdentifier, String> {
    @Override
    public @Nullable String convertToDatabaseColumn(@Nullable ResourceIdentifier resourceIdentifier) {
        if (resourceIdentifier == null) {
            return null;
        }
        return resourceIdentifier.toString();
    }

    @Override
    public @Nullable ResourceIdentifier convertToEntityAttribute(@Nullable String s) {
        if (s == null) {
            return null;
        }
        return ResourceIdentifier.valueOf(s);
    }
}
