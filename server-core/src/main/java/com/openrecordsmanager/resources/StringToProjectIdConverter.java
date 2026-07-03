package com.openrecordsmanager.resources;

import com.openrecordsmanager.api.ResourceIdentifier;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToProjectIdConverter implements Converter<String, ResourceIdentifier> {

    @Override
    public ResourceIdentifier convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }

        return ResourceIdentifier.valueOf(source);
    }
}
