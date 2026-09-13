package com.openrecordsmanager.rest.dto;

import com.openrecordsmanager.api.schema.SchemaField;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InputFormSchemaTest {

    enum SampleMode {
        ALPHA,
        BETA
    }

    record Settings(
            @SchemaField(title = "Mode") SampleMode mode,
            @SchemaField(title = "Name") String name
    ) {
    }

    @Test
    void preservesEnumValuesFromRecordSchema() {
        InputFormSchema schema = InputFormSchema.from(Settings.class);
        InputFormSchemaField mode = schema.properties().get("mode");
        assertNotNull(mode);
        assertEquals("string", mode.type());
        assertEquals(java.util.List.of("ALPHA", "BETA"), mode.enumValues());
    }
}
