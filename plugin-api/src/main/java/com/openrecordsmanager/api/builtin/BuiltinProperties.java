package com.openrecordsmanager.api.builtin;

import com.openrecordsmanager.api.template.property.ObjectPropertyTemplate;
import com.openrecordsmanager.api.template.property.PropertyType;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BuiltinProperties {

    private static final Map<String, ObjectPropertyTemplate<?>> TEMPLATES = new LinkedHashMap<>();
    public static final Map<String, ObjectPropertyTemplate<?>> BUILTIN_PROPERTIES =
            Collections.unmodifiableMap(TEMPLATES);

    public static final ObjectPropertyTemplate<String> NOTES = register(
            BuiltinPropertyIds.NOTES,
            ObjectPropertyTemplate.builder(PropertyType.STRING).build()
    );

    public static final ObjectPropertyTemplate<Instant> DATE_REGISTERED = register(
            BuiltinPropertyIds.DATE_REGISTERED,
            ObjectPropertyTemplate.builder(PropertyType.DATE).build()
    );

    public static final ObjectPropertyTemplate<Instant> DATE_CREATED = register(
            BuiltinPropertyIds.DATE_CREATED,
            ObjectPropertyTemplate.builder(PropertyType.DATE).build()
    );

    public static final ObjectPropertyTemplate<String> KEYWORDS = register(
            BuiltinPropertyIds.KEYWORDS,
            ObjectPropertyTemplate.builder(PropertyType.STRING).build()
    );

    public static final ObjectPropertyTemplate<List<String>> MIME_TYPES = register(
            BuiltinPropertyIds.MIME_TYPES,
            ObjectPropertyTemplate.builder(PropertyType.STRING_LIST).build()
    );

    public static final ObjectPropertyTemplate<String> TITLE = register(
            BuiltinPropertyIds.TITLE,
            ObjectPropertyTemplate.builder(PropertyType.STRING).build()
    );

    public static final ObjectPropertyTemplate<Instant> DATE_MODIFIED = register(
            BuiltinPropertyIds.DATE_MODIFIED,
            ObjectPropertyTemplate.builder(PropertyType.DATE).build()
    );

    public static final ObjectPropertyTemplate<String> GIVEN_NAME = register(
            BuiltinPropertyIds.GIVEN_NAME,
            ObjectPropertyTemplate.builder(PropertyType.STRING).build()
    );

    public static final ObjectPropertyTemplate<String> SURNAME = register(
            BuiltinPropertyIds.SURNAME,
            ObjectPropertyTemplate.builder(PropertyType.STRING).build()
    );

    public static final ObjectPropertyTemplate<String> HONORIFIC = register(
            BuiltinPropertyIds.HONORIFIC,
            ObjectPropertyTemplate.builder(PropertyType.STRING).build()
    );

    public static final ObjectPropertyTemplate<String> EMAIL = register(
            BuiltinPropertyIds.EMAIL,
            ObjectPropertyTemplate.builder(PropertyType.STRING).build()
    );

    public static final ObjectPropertyTemplate<String> USERNAME = register(
            BuiltinPropertyIds.USERNAME,
            ObjectPropertyTemplate.builder(PropertyType.STRING).build()
    );

    public static final ObjectPropertyTemplate<String> NAME = register(
            BuiltinPropertyIds.NAME,
            ObjectPropertyTemplate.builder(PropertyType.STRING).build()
    );

    private BuiltinProperties() {
    }

    private static <T> ObjectPropertyTemplate<T> register(String id, ObjectPropertyTemplate<T> template) {
        TEMPLATES.put(id, template);
        return template;
    }
}
