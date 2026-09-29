package com.openrecordsmanager.api;

/**
 * Standard translation key suffixes for component display fields.
 * Full keys look like {@code {component_type}.{source}.{item}.{field}}.
 */
public enum TranslationField {
    NAME("name"),
    DESCRIPTION("description");

    private final String key;

    TranslationField(String key) {
        this.key = key;
    }

    public String key() {
        return this.key;
    }

    @Override
    public String toString() {
        return this.key;
    }
}
