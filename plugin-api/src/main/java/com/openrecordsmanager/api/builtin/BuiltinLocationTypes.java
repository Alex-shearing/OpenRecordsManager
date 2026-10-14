package com.openrecordsmanager.api.builtin;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.location.LocationKind;
import com.openrecordsmanager.api.template.location.LocationTypeTemplate;
import com.openrecordsmanager.api.types.ComponentTypes;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BuiltinLocationTypes {

    private static final Map<String, LocationTypeTemplate> TEMPLATES = new LinkedHashMap<>();
    public static final Map<String, LocationTypeTemplate> BUILTIN_LOCATION_TYPES =
            Collections.unmodifiableMap(TEMPLATES);

    public static final ComponentReference<LocationTypeTemplate> USER = register(
            BuiltinLocationTypeIds.USER,
            LocationTypeTemplate.builder()
                    .kind(LocationKind.USER)
                    .property(BuiltinProperties.ID)
                    .property(BuiltinProperties.NAME)
                    .property(BuiltinProperties.NOTES)
                    .property(BuiltinProperties.DATE_CREATED)
                    .property(BuiltinProperties.DATE_MODIFIED)
                    .property(BuiltinProperties.GIVEN_NAME)
                    .property(BuiltinProperties.SURNAME)
                    .property(BuiltinProperties.HONORIFIC)
                    .property(BuiltinProperties.EMAIL)
                    .build()
    );

    public static final ComponentReference<LocationTypeTemplate> GROUP = register(
            BuiltinLocationTypeIds.GROUP,
            LocationTypeTemplate.builder()
                    .kind(LocationKind.GROUP)
                    .property(BuiltinProperties.ID)
                    .property(BuiltinProperties.NAME)
                    .property(BuiltinProperties.NOTES)
                    .property(BuiltinProperties.DATE_CREATED)
                    .property(BuiltinProperties.DATE_MODIFIED)
                    .build()
    );

    private BuiltinLocationTypes() {
    }

    private static ComponentReference<LocationTypeTemplate> register(String id, LocationTypeTemplate template) {
        TEMPLATES.put(id, template);
        return ComponentReference.of(
                ComponentTypes.LOCATION_TYPE,
                new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, id)
        );
    }
}
