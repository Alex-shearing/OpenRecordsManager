package com.openrecordsmanager.api.types;

import tools.jackson.databind.util.StdConverter;

public class MaskingConverter extends StdConverter<Object, String> {
    @Override
    public String convert(Object value) {
        return "**********";
    }
}
