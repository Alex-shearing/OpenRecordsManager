package com.openrecordsmanager.api.builtin;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.recordtype.RecordTypeTemplate;
import com.openrecordsmanager.api.template.recordtype.SecurityFilterUsage;
import com.openrecordsmanager.api.types.ComponentTypes;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BuiltinRecordTypes {

    private static final Map<String, RecordTypeTemplate> TEMPLATES = new LinkedHashMap<>();
    public static final Map<String, RecordTypeTemplate> BUILTIN_RECORD_TYPES =
            Collections.unmodifiableMap(TEMPLATES);

    /**
     * Abstract root record type. Shared properties live here; concrete types extend it.
     */
    public static final ComponentReference<RecordTypeTemplate> RECORD = register(
            BuiltinRecordTypeIds.RECORD,
            RecordTypeTemplate.builder()
                    .abstractType(true)
                    .securityFilterUsage(SecurityFilterUsage.SHOW_ALL)
                    .property(BuiltinProperties.ID)
                    .property(BuiltinProperties.TITLE)
                    .property(BuiltinProperties.NOTES)
                    .property(BuiltinProperties.KEYWORDS)
                    .property(BuiltinProperties.DATE_CREATED)
                    .property(BuiltinProperties.DATE_MODIFIED)
                    .property(BuiltinProperties.DATE_REGISTERED)
                    .build()
    );

    /**
     * Structural record used as a parent for other records via {@code contained_by}.
     * No file content is allowed.
     */
    public static final ComponentReference<RecordTypeTemplate> CONTAINER = register(
            BuiltinRecordTypeIds.CONTAINER,
            RecordTypeTemplate.builder()
                    .baseType(RECORD)
                    .securityFilterUsage(SecurityFilterUsage.SHOW_ALL)
                    .build()
    );

    private BuiltinRecordTypes() {
    }

    private static ComponentReference<RecordTypeTemplate> register(String id, RecordTypeTemplate template) {
        TEMPLATES.put(id, template);
        return ComponentReference.of(
                ComponentTypes.RECORD_TYPE,
                new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, id)
        );
    }
}
