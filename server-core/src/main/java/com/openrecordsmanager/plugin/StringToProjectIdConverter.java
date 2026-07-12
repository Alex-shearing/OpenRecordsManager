package com.openrecordsmanager.plugin;

import com.openrecordsmanager.api.ResourceIdentifier;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToProjectIdConverter implements Converter<String, ResourceIdentifier> {

    @Override
    public ResourceIdentifier convert(String source) {
        return ResourceIdentifier.valueOf(source);
    }
}
