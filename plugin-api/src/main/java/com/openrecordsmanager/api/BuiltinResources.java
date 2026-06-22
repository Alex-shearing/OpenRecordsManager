package com.openrecordsmanager.api;

import com.openrecordsmanager.api.property.PropertyDefinition;
import com.openrecordsmanager.api.property.PropertyType;

import java.util.Date;

public class BuiltinResources implements Plugin {
    public static final PropertyDefinition<String> NOTES = PropertyDefinition.builder("notes", PropertyType.STRING)
            .name("Notes")
            .description("Notes on the object")
            .build();

    public static final PropertyDefinition<Date> DATE_REGISTERED = PropertyDefinition.builder("date_registered", PropertyType.DATE)
            .name("Date Registered")
            .description("Date the object was registered into the tool")
            .build();

    public static final PropertyDefinition<Date> DATE_CREATED = PropertyDefinition.builder("date_created", PropertyType.DATE)
            .name("Date Created")
            .description("Date the record was created")
            .build();

    public static final PropertyDefinition<String> KEYWORDS = PropertyDefinition.builder("keywords", PropertyType.STRING)
            .name("Keywords")
            .description("Relevant keywords that can assist in searching")
            .build();

    public static final PropertyDefinition<String> MIME_TYPE = PropertyDefinition.builder("mime_type", PropertyType.STRING)
            .name("MIME Type")
            .description("Standardised internet types defining a file type")
            .build();

    @Override
    public String getName() {
        return "builtin";
    }

    @Override
    public void initialise(PluginContext registry) {
        registry.registerComponents(
                NOTES, DATE_REGISTERED, DATE_CREATED, KEYWORDS, MIME_TYPE
        );
    }
}
