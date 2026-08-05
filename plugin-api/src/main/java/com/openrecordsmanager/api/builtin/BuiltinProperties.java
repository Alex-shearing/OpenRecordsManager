package com.openrecordsmanager.api.builtin;

import com.openrecordsmanager.api.template.property.ObjectPropertyTemplate;
import com.openrecordsmanager.api.template.property.PropertyType;

import java.util.Date;

public class BuiltinProperties {
    public static final ObjectPropertyTemplate<String> NOTES = ObjectPropertyTemplate.builder("Notes", PropertyType.STRING)
            .description("Notes on the object")
            .build();

    public static final ObjectPropertyTemplate<Date> DATE_REGISTERED = ObjectPropertyTemplate.builder("Date Registered", PropertyType.DATE)
            .description("Date the object was registered")
            .build();

    public static final ObjectPropertyTemplate<Date> DATE_CREATED = ObjectPropertyTemplate.builder("Date Created", PropertyType.DATE)
            .description("Date the record was created")
            .build();

    public static final ObjectPropertyTemplate<String> KEYWORDS = ObjectPropertyTemplate.builder("Keywords", PropertyType.STRING)
            .description("Relevant keywords that can assist in searching")
            .build();

    public static final ObjectPropertyTemplate<String> MIME_TYPE = ObjectPropertyTemplate.builder("MIME Type", PropertyType.STRING)
            .description("Standardised internet types defining a file type")
            .build();
}
