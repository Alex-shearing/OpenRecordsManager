package com.openrecordsmanager.api.builtin;

import com.openrecordsmanager.api.template.property.PropertyDefinition;
import com.openrecordsmanager.api.template.property.PropertyType;

import java.util.Date;

public class BuiltinProperties {
    public static final PropertyDefinition<String> NOTES = PropertyDefinition.builder("Notes", PropertyType.STRING)
            .description("Notes on the object")
            .build();

    public static final PropertyDefinition<Date> DATE_REGISTERED = PropertyDefinition.builder("Date Registered", PropertyType.DATE)
            .description("Date the object was registered")
            .build();

    public static final PropertyDefinition<Date> DATE_CREATED = PropertyDefinition.builder("Date Created", PropertyType.DATE)
            .description("Date the record was created")
            .build();

    public static final PropertyDefinition<String> KEYWORDS = PropertyDefinition.builder("Keywords", PropertyType.STRING)
            .description("Relevant keywords that can assist in searching")
            .build();
    
    public static final PropertyDefinition<String> MIME_TYPE = PropertyDefinition.builder("MIME Type", PropertyType.STRING)
            .description("Standardised internet types defining a file type")
            .build();
}
