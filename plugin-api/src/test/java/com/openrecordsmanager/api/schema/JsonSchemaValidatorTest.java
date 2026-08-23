package com.openrecordsmanager.api.schema;

import com.openrecordsmanager.api.errors.InputValidationException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonSchemaValidatorTest {

    public record LoginInputs(
            @SchemaField(title = "Username", minLength = 1) String username,
            @SchemaField(title = "Password", format = SchemaFieldFormat.PASSWORD, minLength = 1) String password
    ) {
    }

    @Test
    void generatesJsonSchemaFromRecord() {
        JsonNode schema = JsonSchemaValidator.getSchema(LoginInputs.class).getSchemaNode();

        assertEquals("object", schema.get("type").stringValue());
        assertFalse(schema.get("additionalProperties").booleanValue());
        assertEquals("Username", schema.get("properties").get("username").get("title").stringValue());
        assertEquals(1, schema.get("properties").get("username").get("minLength").intValue());
        assertTrue(schema.get("properties").get("password").get("writeOnly").booleanValue());

        JsonNode required = schema.get("required");
        assertEquals(2, required.size());
        assertTrue(required.toString().contains("username"));
        assertTrue(required.toString().contains("password"));
    }

    enum SampleEnum {ALPHA, BETA}

    public record EnumInputs(@SchemaField(title = "Mode") SampleEnum mode) {
    }

    @Test
    void generatesEnumSchema() {
        JsonNode schema = JsonSchemaValidator.getSchema(EnumInputs.class).getSchemaNode();
        JsonNode mode = schema.get("properties").get("mode");

        assertEquals("string", mode.get("type").stringValue());
        assertTrue(mode.get("enum").toString().contains("ALPHA"));
        assertTrue(mode.get("enum").toString().contains("BETA"));
    }

    @Test
    void validInputNormalizesTrimmedValues() {
        Map<String, Object> result = JsonSchemaValidator.validateAndSerialize(LoginInputs.class, Map.of(
                "username", "  alice  ",
                "password", "secret"
        ));

        assertEquals("alice", result.get("username"));
        assertEquals("secret", result.get("password"));
    }

    @Test
    void missingRequiredFieldsReturnFieldErrors() {
        assertThrows(InputValidationException.class, () -> {
            JsonSchemaValidator.validateAndSerialize(LoginInputs.class, Map.of("username", "alice"));
        });
    }

    @Test
    void extraPropertiesRejected() {
        assertThrows(InputValidationException.class, () -> {
            JsonSchemaValidator.validateAndSerialize(LoginInputs.class, Map.of(
                    "username", "alice",
                    "password", "secret",
                    "extra", "nope"
            ));
        });
    }
}
