package com.openrecordsmanager.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InputFormSchemaTest {

    enum SampleMode {
        ALPHA,
        BETA
    }

    record Settings(
            @Schema(title = "Mode") SampleMode mode,
            @Schema(title = "Name") String name
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
