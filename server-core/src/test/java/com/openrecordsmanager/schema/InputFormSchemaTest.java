package com.openrecordsmanager.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InputFormSchemaTest {

    enum SampleMode {
        ALPHA,
        BETA
    }

    record Settings(
            @Schema(title = "test.settings.schema.mode.title")
            SampleMode mode,
            @Schema(
                    title = "test.settings.schema.name.title",
                    description = "test.settings.schema.name.description"
            )
            String name
    ) {
    }

    @Test
    void preservesEnumValuesFromRecordSchema() {
        InputFormSchema schema = InputFormSchema.from(Settings.class);
        InputFormSchema.InputFormSchemaField mode = schema.properties().get("mode");
        assertNotNull(mode);
        assertEquals("string", mode.type());
        assertEquals(List.of("ALPHA", "BETA"), mode.enumValues());
    }

    @Test
    void usesSchemaTitleAndDescriptionAsMessageKeys() {
        InputFormSchema schema = InputFormSchema.from(Settings.class);

        InputFormSchema.InputFormSchemaField mode = schema.properties().get("mode");
        assertEquals("test.settings.schema.mode.title", mode.title());
        assertNull(mode.description());

        InputFormSchema.InputFormSchemaField name = schema.properties().get("name");
        assertEquals("test.settings.schema.name.title", name.title());
        assertEquals("test.settings.schema.name.description", name.description());
    }
}
