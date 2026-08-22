package com.openrecordsmanager.api.schema;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputFormSchemaTest {

    public record LoginInputs(
            @SchemaField(title = "Username", minLength = 1) String username,
            @SchemaField(title = "Password", format = SchemaFieldFormat.PASSWORD, minLength = 1) String password
    ) {
    }

    @Test
    void fromRecordClassProducesFormSchema() {
        InputFormSchema schema = InputFormSchema.from(LoginInputs.class);

        assertEquals("object", schema.type());
        assertFalse(schema.additionalProperties());
        assertNotNull(schema.properties());
        assertEquals("Username", schema.properties().get("username").title());
        assertTrue(schema.properties().get("password").writeOnly());
        assertTrue(schema.required().contains("username"));
        assertTrue(schema.required().contains("password"));
    }
}
