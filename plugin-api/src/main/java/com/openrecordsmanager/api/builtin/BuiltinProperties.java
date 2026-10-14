package com.openrecordsmanager.api.builtin;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.property.ObjectPropertyTemplate;
import com.openrecordsmanager.api.template.property.PropertyType;
import com.openrecordsmanager.api.types.ComponentTypes;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class BuiltinProperties {

    private static final Map<String, ObjectPropertyTemplate<?>> TEMPLATES = new LinkedHashMap<>();
    public static final Map<String, ObjectPropertyTemplate<?>> BUILTIN_PROPERTIES =
            Collections.unmodifiableMap(TEMPLATES);

    public static final ComponentReference<ObjectPropertyTemplate<UUID>> ID = register(
            BuiltinPropertyIds.ID,
            ObjectPropertyTemplate.builder(PropertyType.UUID).build()
    );

    public static final ComponentReference<ObjectPropertyTemplate<String>> NOTES = register(
            BuiltinPropertyIds.NOTES,
            ObjectPropertyTemplate.builder(PropertyType.STRING).build()
    );

    public static final ComponentReference<ObjectPropertyTemplate<Instant>> DATE_REGISTERED = register(
            BuiltinPropertyIds.DATE_REGISTERED,
            ObjectPropertyTemplate.builder(PropertyType.DATE).build()
    );

    public static final ComponentReference<ObjectPropertyTemplate<Instant>> DATE_CREATED = register(
            BuiltinPropertyIds.DATE_CREATED,
            ObjectPropertyTemplate.builder(PropertyType.DATE).build()
    );

    public static final ComponentReference<ObjectPropertyTemplate<String>> KEYWORDS = register(
            BuiltinPropertyIds.KEYWORDS,
            ObjectPropertyTemplate.builder(PropertyType.STRING).build()
    );

    public static final ComponentReference<ObjectPropertyTemplate<String>> TITLE = register(
            BuiltinPropertyIds.TITLE,
            ObjectPropertyTemplate.builder(PropertyType.STRING).build()
    );

    public static final ComponentReference<ObjectPropertyTemplate<Instant>> DATE_MODIFIED = register(
            BuiltinPropertyIds.DATE_MODIFIED,
            ObjectPropertyTemplate.builder(PropertyType.DATE).build()
    );

    public static final ComponentReference<ObjectPropertyTemplate<String>> GIVEN_NAME = register(
            BuiltinPropertyIds.GIVEN_NAME,
            ObjectPropertyTemplate.builder(PropertyType.STRING).build()
    );

    public static final ComponentReference<ObjectPropertyTemplate<String>> SURNAME = register(
            BuiltinPropertyIds.SURNAME,
            ObjectPropertyTemplate.builder(PropertyType.STRING).build()
    );

    public static final ComponentReference<ObjectPropertyTemplate<String>> HONORIFIC = register(
            BuiltinPropertyIds.HONORIFIC,
            ObjectPropertyTemplate.builder(PropertyType.STRING).build()
    );

    public static final ComponentReference<ObjectPropertyTemplate<String>> EMAIL = register(
            BuiltinPropertyIds.EMAIL,
            ObjectPropertyTemplate.builder(PropertyType.STRING).build()
    );

    public static final ComponentReference<ObjectPropertyTemplate<String>> NAME = register(
            BuiltinPropertyIds.NAME,
            ObjectPropertyTemplate.builder(PropertyType.STRING).build()
    );

    private BuiltinProperties() {
    }

    @SuppressWarnings("unchecked")
    private static <T> ComponentReference<ObjectPropertyTemplate<T>> register(
            String id,
            ObjectPropertyTemplate<T> template
    ) {
        TEMPLATES.put(id, template);
        return (ComponentReference<ObjectPropertyTemplate<T>>) (ComponentReference<?>) ComponentReference.of(
                ComponentTypes.OBJECT_PROPERTY,
                new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, id)
        );
    }
}
