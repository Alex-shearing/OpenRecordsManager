package com.openrecordsmanager.api.builtin;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.location.LocationKind;
import com.openrecordsmanager.api.template.location.LocationRelationshipTypeTemplate;
import com.openrecordsmanager.api.types.ComponentTypes;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BuiltinRelationshipTypes {

    private static final Map<String, LocationRelationshipTypeTemplate> TEMPLATES = new LinkedHashMap<>();
    public static final Map<String, LocationRelationshipTypeTemplate> BUILTIN_RELATIONSHIP_TYPES =
            Collections.unmodifiableMap(TEMPLATES);

    public static final ComponentReference<LocationRelationshipTypeTemplate> MEMBER_OF = register(
            BuiltinRelationshipTypeIds.MEMBER_OF,
            LocationRelationshipTypeTemplate.builder()
                    .sourceKind(LocationKind.ANY)
                    .targetKind(LocationKind.GROUP)
                    .uniquePerSource(false)
                    .build()
    );

    public static final ComponentReference<LocationRelationshipTypeTemplate> REPORTS_TO = register(
            BuiltinRelationshipTypeIds.REPORTS_TO,
            LocationRelationshipTypeTemplate.builder()
                    .sourceKind(LocationKind.ANY)
                    .targetKind(LocationKind.ANY)
                    .uniquePerSource(true)
                    .build()
    );

    public static final ComponentReference<LocationRelationshipTypeTemplate> DEPUTY_OF = register(
            BuiltinRelationshipTypeIds.DEPUTY_OF,
            LocationRelationshipTypeTemplate.builder()
                    .sourceKind(LocationKind.ANY)
                    .targetKind(LocationKind.ANY)
                    .uniquePerSource(false)
                    .build()
    );

    public static final ComponentReference<LocationRelationshipTypeTemplate> ACTING_FOR = register(
            BuiltinRelationshipTypeIds.ACTING_FOR,
            LocationRelationshipTypeTemplate.builder()
                    .sourceKind(LocationKind.ANY)
                    .targetKind(LocationKind.ANY)
                    .uniquePerSource(false)
                    .build()
    );

    private BuiltinRelationshipTypes() {
    }

    private static ComponentReference<LocationRelationshipTypeTemplate> register(
            String id,
            LocationRelationshipTypeTemplate template
    ) {
        TEMPLATES.put(id, template);
        return ComponentReference.of(
                ComponentTypes.LOCATION_RELATIONSHIP_TYPE,
                new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, id)
        );
    }
}
