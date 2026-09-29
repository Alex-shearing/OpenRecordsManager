package com.openrecordsmanager.api.builtin;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.property.ObjectPropertyTemplate;
import com.openrecordsmanager.api.template.property.PropertyType;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BuiltinProperties {

    public static final ObjectPropertyTemplate<String> NOTES = ObjectPropertyTemplate.builder(PropertyType.STRING)
            .build();

    public static final ObjectPropertyTemplate<Instant> DATE_REGISTERED = ObjectPropertyTemplate.builder(PropertyType.DATE)
            .build();

    public static final ObjectPropertyTemplate<Instant> DATE_CREATED = ObjectPropertyTemplate.builder(PropertyType.DATE)
            .build();

    public static final ObjectPropertyTemplate<String> KEYWORDS = ObjectPropertyTemplate.builder(PropertyType.STRING)
            .build();

    public static final ObjectPropertyTemplate<List<String>> MIME_TYPES = ObjectPropertyTemplate.builder(PropertyType.STRING_LIST)
            .build();

    public static final ObjectPropertyTemplate<String> TITLE = ObjectPropertyTemplate.builder(PropertyType.STRING)
            .build();

    public static final ObjectPropertyTemplate<Instant> DATE_MODIFIED = ObjectPropertyTemplate.builder(PropertyType.DATE)
            .build();

    public static final ObjectPropertyTemplate<String> GIVEN_NAME = ObjectPropertyTemplate.builder(PropertyType.STRING)
            .build();

    public static final ObjectPropertyTemplate<String> SURNAME = ObjectPropertyTemplate.builder(PropertyType.STRING)
            .build();

    public static final ObjectPropertyTemplate<String> HONORIFIC = ObjectPropertyTemplate.builder(PropertyType.STRING)
            .build();

    public static final ObjectPropertyTemplate<String> EMAIL = ObjectPropertyTemplate.builder(PropertyType.STRING)
            .build();

    public static final ObjectPropertyTemplate<String> USERNAME = ObjectPropertyTemplate.builder(PropertyType.STRING)
            .build();

    public static final Map<ResourceIdentifier, ObjectPropertyTemplate<?>> BUILTIN_PROPERTIES;

    static {
        Map<ResourceIdentifier, ObjectPropertyTemplate<?>> templates = new LinkedHashMap<>();
        templates.put(BuiltinPropertyIds.NOTES, NOTES);
        templates.put(BuiltinPropertyIds.DATE_REGISTERED, DATE_REGISTERED);
        templates.put(BuiltinPropertyIds.DATE_CREATED, DATE_CREATED);
        templates.put(BuiltinPropertyIds.KEYWORDS, KEYWORDS);
        templates.put(BuiltinPropertyIds.MIME_TYPES, MIME_TYPES);
        templates.put(BuiltinPropertyIds.TITLE, TITLE);
        templates.put(BuiltinPropertyIds.DATE_MODIFIED, DATE_MODIFIED);
        templates.put(BuiltinPropertyIds.GIVEN_NAME, GIVEN_NAME);
        templates.put(BuiltinPropertyIds.SURNAME, SURNAME);
        templates.put(BuiltinPropertyIds.HONORIFIC, HONORIFIC);
        templates.put(BuiltinPropertyIds.EMAIL, EMAIL);
        templates.put(BuiltinPropertyIds.USERNAME, USERNAME);
        BUILTIN_PROPERTIES = Map.copyOf(templates);
    }

    private BuiltinProperties() {
    }
}
