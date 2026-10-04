package com.openrecordsmanager.api.builtin;

import com.openrecordsmanager.api.location.LocationKind;
import com.openrecordsmanager.api.template.location.LocationTypeTemplate;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BuiltinLocationTypes {

    private static final Map<String, LocationTypeTemplate> TEMPLATES = new LinkedHashMap<>();
    public static final Map<String, LocationTypeTemplate> BUILTIN_LOCATION_TYPES =
            Collections.unmodifiableMap(TEMPLATES);

    public static final LocationTypeTemplate USER = register(
            BuiltinLocationTypeIds.USER,
            LocationTypeTemplate.builder()
                    .kind(LocationKind.USER)
                    .property(BuiltinProperties.NAME)
                    .property(BuiltinProperties.NOTES)
                    .property(BuiltinProperties.DATE_CREATED)
                    .property(BuiltinProperties.DATE_MODIFIED)
                    .property(BuiltinProperties.USERNAME)
                    .property(BuiltinProperties.GIVEN_NAME)
                    .property(BuiltinProperties.SURNAME)
                    .property(BuiltinProperties.HONORIFIC)
                    .property(BuiltinProperties.EMAIL)
                    .build()
    );

    public static final LocationTypeTemplate GROUP = register(
            BuiltinLocationTypeIds.GROUP,
            LocationTypeTemplate.builder()
                    .kind(LocationKind.GROUP)
                    .property(BuiltinProperties.NAME)
                    .property(BuiltinProperties.NOTES)
                    .property(BuiltinProperties.DATE_CREATED)
                    .property(BuiltinProperties.DATE_MODIFIED)
                    .build()
    );

    private BuiltinLocationTypes() {
    }

    private static LocationTypeTemplate register(String id, LocationTypeTemplate template) {
        TEMPLATES.put(id, template);
        return template;
    }
}
