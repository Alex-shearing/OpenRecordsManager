package com.openrecordsmanager.api.builtin;

import com.openrecordsmanager.api.ResourceIdentifier;

/**
 * Builtin property item names and {@link ResourceIdentifier}s.
 * String constants use the {@code _ID} suffix; identifiers use the property name.
 */
public final class BuiltinPropertyIds {

    public static final String NOTES_ID = "notes";
    public static final ResourceIdentifier NOTES = id(NOTES_ID);

    public static final String DATE_REGISTERED_ID = "date_registered";
    public static final ResourceIdentifier DATE_REGISTERED = id(DATE_REGISTERED_ID);

    public static final String DATE_CREATED_ID = "date_created";
    public static final ResourceIdentifier DATE_CREATED = id(DATE_CREATED_ID);

    public static final String KEYWORDS_ID = "keywords";
    public static final ResourceIdentifier KEYWORDS = id(KEYWORDS_ID);

    public static final String MIME_TYPES_ID = "mime_types";
    public static final ResourceIdentifier MIME_TYPES = id(MIME_TYPES_ID);

    public static final String TITLE_ID = "title";
    public static final ResourceIdentifier TITLE = id(TITLE_ID);

    public static final String DATE_MODIFIED_ID = "date_modified";
    public static final ResourceIdentifier DATE_MODIFIED = id(DATE_MODIFIED_ID);

    public static final String GIVEN_NAME_ID = "given_name";
    public static final ResourceIdentifier GIVEN_NAME = id(GIVEN_NAME_ID);

    public static final String SURNAME_ID = "surname";
    public static final ResourceIdentifier SURNAME = id(SURNAME_ID);

    public static final String HONORIFIC_ID = "honorific";
    public static final ResourceIdentifier HONORIFIC = id(HONORIFIC_ID);

    public static final String EMAIL_ID = "email";
    public static final ResourceIdentifier EMAIL = id(EMAIL_ID);

    public static final String USERNAME_ID = "username";
    public static final ResourceIdentifier USERNAME = id(USERNAME_ID);

    private BuiltinPropertyIds() {
    }

    public static ResourceIdentifier id(String item) {
        return new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, item);
    }
}
