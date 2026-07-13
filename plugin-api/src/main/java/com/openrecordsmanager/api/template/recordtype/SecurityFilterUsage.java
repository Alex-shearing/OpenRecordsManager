package com.openrecordsmanager.api.template.recordtype;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

public enum SecurityFilterUsage {
    /**
     * Hides the fact the record exists at all from a user who does not pass the security filter.
     */
    HIDE_RECORD,
    /**
     * Hides attached files for a user who does not pass the security filter.
     */
    HIDE_FILES,
    /**
     * Does not hide any aspect of the record for a user who does not pass the security filter.
     */
    SHOW_ALL;

    public boolean canSeeMetadata() {
        return this != HIDE_RECORD;
    }

    public boolean canSeeFiles() {
        return this != HIDE_RECORD && this != HIDE_FILES;
    }

    public static class Deserializer extends StdDeserializer<SecurityFilterUsage> {

        public Deserializer() {
            super(SecurityFilterUsage.class);
        }

        @Override
        public SecurityFilterUsage deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
            return SecurityFilterUsage.valueOf(p.getValueAsString().toUpperCase());
        }

        @Override
        public Object getNullValue(DeserializationContext ctxt) {
            return HIDE_FILES;
        }
    }

}
