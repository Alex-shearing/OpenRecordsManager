package com.openrecordsmanager.api.builtin;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.relationship.RelationshipTypeTemplate;
import com.openrecordsmanager.api.types.ComponentTypes;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BuiltinRelationshipTypes {

    private static final Map<String, RelationshipTypeTemplate> TEMPLATES = new LinkedHashMap<>();
    public static final Map<String, RelationshipTypeTemplate> BUILTIN_RELATIONSHIP_TYPES =
            Collections.unmodifiableMap(TEMPLATES);

    // Location to location

    public static final ComponentReference<RelationshipTypeTemplate> MEMBER_OF = register(
            BuiltinRelationshipTypeIds.MEMBER_OF,
            RelationshipTypeTemplate.builder()
                    .sourceType(BuiltinLocationTypes.LOCATION)
                    .targetType(BuiltinLocationTypes.GROUP)
                    .uniquePerSource(false)
                    .build()
    );

    public static final ComponentReference<RelationshipTypeTemplate> REPORTS_TO = register(
            BuiltinRelationshipTypeIds.REPORTS_TO,
            RelationshipTypeTemplate.builder()
                    .sourceType(BuiltinLocationTypes.LOCATION)
                    .targetType(BuiltinLocationTypes.LOCATION)
                    .uniquePerSource(true)
                    .build()
    );

    public static final ComponentReference<RelationshipTypeTemplate> DEPUTY_OF = register(
            BuiltinRelationshipTypeIds.DEPUTY_OF,
            RelationshipTypeTemplate.builder()
                    .sourceType(BuiltinLocationTypes.LOCATION)
                    .targetType(BuiltinLocationTypes.LOCATION)
                    .uniquePerSource(false)
                    .build()
    );

    public static final ComponentReference<RelationshipTypeTemplate> ACTING_FOR = register(
            BuiltinRelationshipTypeIds.ACTING_FOR,
            RelationshipTypeTemplate.builder()
                    .sourceType(BuiltinLocationTypes.LOCATION)
                    .targetType(BuiltinLocationTypes.LOCATION)
                    .uniquePerSource(false)
                    .build()
    );

    // Record to record

    public static final ComponentReference<RelationshipTypeTemplate> RELATED_TO = register(
            BuiltinRelationshipTypeIds.RELATED_TO,
            RelationshipTypeTemplate.builder()
                    .sourceType(BuiltinRecordTypes.RECORD)
                    .targetType(BuiltinRecordTypes.RECORD)
                    .uniquePerSource(false)
                    .build()
    );

    public static final ComponentReference<RelationshipTypeTemplate> SUPERSEDES = register(
            BuiltinRelationshipTypeIds.SUPERSEDES,
            RelationshipTypeTemplate.builder()
                    .sourceType(BuiltinRecordTypes.RECORD)
                    .targetType(BuiltinRecordTypes.RECORD)
                    .uniquePerSource(true)
                    .build()
    );

    /**
     * Source record is contained by a target {@link BuiltinRecordTypes#CONTAINER}.
     * Unique per source so a record has at most one active container parent.
     */
    public static final ComponentReference<RelationshipTypeTemplate> CONTAINED_BY = register(
            BuiltinRelationshipTypeIds.CONTAINED_BY,
            RelationshipTypeTemplate.builder()
                    .sourceType(BuiltinRecordTypes.RECORD)
                    .targetType(BuiltinRecordTypes.CONTAINER)
                    .uniquePerSource(true)
                    .build()
    );

    // Location to record

    public static final ComponentReference<RelationshipTypeTemplate> CUSTODIAN_OF = register(
            BuiltinRelationshipTypeIds.CUSTODIAN_OF,
            RelationshipTypeTemplate.builder()
                    .sourceType(BuiltinLocationTypes.LOCATION)
                    .targetType(BuiltinRecordTypes.RECORD)
                    .uniquePerSource(false)
                    .build()
    );

    public static final ComponentReference<RelationshipTypeTemplate> CREATED_BY = register(
            BuiltinRelationshipTypeIds.CREATED_BY,
            RelationshipTypeTemplate.builder()
                    .sourceType(BuiltinRecordTypes.RECORD)
                    .targetType(BuiltinLocationTypes.LOCATION)
                    .uniquePerSource(false)
                    .build()
    );

    private BuiltinRelationshipTypes() {
    }

    private static ComponentReference<RelationshipTypeTemplate> register(
            String id,
            RelationshipTypeTemplate template
    ) {
        TEMPLATES.put(id, template);
        return ComponentReference.of(
                ComponentTypes.RELATIONSHIP_TYPE,
                new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, id)
        );
    }
}
