package com.openrecordsmanager.api.builtin;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.property.ObjectPropertyTemplate;
import com.openrecordsmanager.api.template.property.PropertyType;

import java.time.Instant;
import java.util.List;

public final class BuiltinProperties {

    public static final ResourceIdentifier NOTES_ID = id("notes");
    public static final ObjectPropertyTemplate<String> NOTES = ObjectPropertyTemplate.builder("Notes", PropertyType.STRING)
            .description("Notes on the object")
            .build();

    public static final ResourceIdentifier DATE_REGISTERED_ID = id("date_registered");
    public static final ObjectPropertyTemplate<Instant> DATE_REGISTERED = ObjectPropertyTemplate.builder("Date Registered", PropertyType.DATE)
            .description("Date the object was registered")
            .build();

    public static final ResourceIdentifier DATE_CREATED_ID = id("date_created");
    public static final ObjectPropertyTemplate<Instant> DATE_CREATED = ObjectPropertyTemplate.builder("Date Created", PropertyType.DATE)
            .description("Date the object was created")
            .build();

    public static final ResourceIdentifier KEYWORDS_ID = id("keywords");
    public static final ObjectPropertyTemplate<String> KEYWORDS = ObjectPropertyTemplate.builder("Keywords", PropertyType.STRING)
            .description("Relevant keywords that can assist in searching")
            .build();

    public static final ResourceIdentifier MIME_TYPES_ID = id("mime_types");
    public static final ObjectPropertyTemplate<List<String>> MIME_TYPES = ObjectPropertyTemplate.builder("MIME Types", PropertyType.STRING_LIST)
            .description("Standardised internet types defining file types")
            .build();

    public static final ResourceIdentifier TITLE_ID = id("title");
    public static final ObjectPropertyTemplate<String> TITLE = ObjectPropertyTemplate.builder("Title", PropertyType.STRING)
            .description("Title of the record")
            .build();

    public static final ResourceIdentifier DATE_MODIFIED_ID = id("date_modified");
    public static final ObjectPropertyTemplate<Instant> DATE_MODIFIED = ObjectPropertyTemplate.builder("Date Modified", PropertyType.DATE)
            .description("Date the object was last modified")
            .build();

    public static final ResourceIdentifier GIVEN_NAME_ID = id("given_name");
    public static final ObjectPropertyTemplate<String> GIVEN_NAME = ObjectPropertyTemplate.builder("Given Name", PropertyType.STRING)
            .description("Given name of the user")
            .build();

    public static final ResourceIdentifier SURNAME_ID = id("surname");
    public static final ObjectPropertyTemplate<String> SURNAME = ObjectPropertyTemplate.builder("Surname", PropertyType.STRING)
            .description("Surname of the user")
            .build();

    public static final ResourceIdentifier HONORIFIC_ID = id("honorific");
    public static final ObjectPropertyTemplate<String> HONORIFIC = ObjectPropertyTemplate.builder("Honorific", PropertyType.STRING)
            .description("Honorific prefix for the user")
            .build();

    public static final ResourceIdentifier EMAIL_ID = id("email");
    public static final ObjectPropertyTemplate<String> EMAIL = ObjectPropertyTemplate.builder("Email", PropertyType.STRING)
            .description("Email address of the user")
            .build();

    private BuiltinProperties() {
    }

    public static ResourceIdentifier id(String item) {
        return new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, item);
    }
}
