package com.openrecordsmanager.api.builtin;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.property.ObjectPropertyTemplate;
import com.openrecordsmanager.api.template.property.PropertyType;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BuiltinProperties {

    public static final ObjectPropertyTemplate<String> NOTES = ObjectPropertyTemplate.builder("Notes", PropertyType.STRING)
            .description("Notes on the object")
            .build();

    public static final ObjectPropertyTemplate<Instant> DATE_REGISTERED = ObjectPropertyTemplate.builder("Date Registered", PropertyType.DATE)
            .description("Date the object was registered")
            .build();

    public static final ObjectPropertyTemplate<Instant> DATE_CREATED = ObjectPropertyTemplate.builder("Date Created", PropertyType.DATE)
            .description("Date the object was created")
            .build();

    public static final ObjectPropertyTemplate<String> KEYWORDS = ObjectPropertyTemplate.builder("Keywords", PropertyType.STRING)
            .description("Relevant keywords that can assist in searching")
            .build();

    public static final ObjectPropertyTemplate<List<String>> MIME_TYPES = ObjectPropertyTemplate.builder("MIME Types", PropertyType.STRING_LIST)
            .description("Standardised internet types defining file types")
            .build();

    public static final ObjectPropertyTemplate<String> TITLE = ObjectPropertyTemplate.builder("Title", PropertyType.STRING)
            .description("Title of the record")
            .build();

    public static final ObjectPropertyTemplate<Instant> DATE_MODIFIED = ObjectPropertyTemplate.builder("Date Modified", PropertyType.DATE)
            .description("Date the object was last modified")
            .build();

    public static final ObjectPropertyTemplate<String> GIVEN_NAME = ObjectPropertyTemplate.builder("Given Name", PropertyType.STRING)
            .description("Given name of the user")
            .build();

    public static final ObjectPropertyTemplate<String> SURNAME = ObjectPropertyTemplate.builder("Surname", PropertyType.STRING)
            .description("Surname of the user")
            .build();

    public static final ObjectPropertyTemplate<String> HONORIFIC = ObjectPropertyTemplate.builder("Honorific", PropertyType.STRING)
            .description("Honorific prefix for the user")
            .build();

    public static final ObjectPropertyTemplate<String> EMAIL = ObjectPropertyTemplate.builder("Email", PropertyType.STRING)
            .description("Email address of the user")
            .build();

    public static final ObjectPropertyTemplate<String> USERNAME = ObjectPropertyTemplate.builder("Username", PropertyType.STRING)
            .description("Login username of the user")
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
