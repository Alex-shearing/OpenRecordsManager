package com.openrecordsmanager.api.schema;

import com.openrecordsmanager.api.errors.InputValidationException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonSchemaValidatorTest {

    public record LoginInputs(
            @SchemaField(title = "Username", minLength = 1) String username,
            @SchemaField(title = "Password", format = SchemaFieldFormat.PASSWORD, minLength = 1) String password
    ) {
    }

    public record ExplicitWriteOnlyInputs(
            @SchemaField(title = "Name", minLength = 1) String name,
            @SchemaField(title = "Token", writeOnly = true, minLength = 1) String token
    ) {
    }

    public record SecretKeyInputs(
            @SchemaField(title = "Label", minLength = 1) String label,
            @SchemaField(title = "Secret Key", format = SchemaFieldFormat.PASSWORD) byte[] secretKey
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

    @Test
    void explicitWriteOnlySetsSchemaFlag() {
        JsonNode schema = JsonSchemaValidator.getSchema(ExplicitWriteOnlyInputs.class).getSchemaNode();
        assertTrue(schema.get("properties").get("token").get("writeOnly").booleanValue());
        assertNull(schema.get("properties").get("name").get("writeOnly"));
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
        assertThrows(InputValidationException.class, () ->
                JsonSchemaValidator.validateAndSerialize(LoginInputs.class, Map.of("username", "alice"))
        );
    }

    @Test
    void extraPropertiesRejected() {
        assertThrows(InputValidationException.class, () ->
                JsonSchemaValidator.validateAndSerialize(LoginInputs.class, Map.of(
                        "username", "alice",
                        "password", "secret",
                        "extra", "nope"
                ))
        );
    }

    @Test
    void serializeSettingsForClientOmitsPasswordFields() {
        Map<String, ?> client = JsonSchemaValidator.serializeSettingsForClient(
                new LoginInputs("alice", "secret")
        );

        assertEquals("alice", client.get("username"));
        assertFalse(client.containsKey("password"));
    }

    @Test
    void serializeSettingsForClientOmitsExplicitWriteOnlyAndPasswordBytes() {
        Map<String, ?> tokenClient = JsonSchemaValidator.serializeSettingsForClient(
                new ExplicitWriteOnlyInputs("svc", "tok-123")
        );
        assertEquals("svc", tokenClient.get("name"));
        assertFalse(tokenClient.containsKey("token"));

        Map<String, ?> secretClient = JsonSchemaValidator.serializeSettingsForClient(
                new SecretKeyInputs("main", new byte[]{1, 2, 3})
        );
        assertEquals("main", secretClient.get("label"));
        assertFalse(secretClient.containsKey("secretKey"));
    }

    @Test
    void mergeWriteOnlyFromExistingFillsBlankPassword() {
        Map<String, Object> incoming = new HashMap<>();
        incoming.put("username", "alice");
        incoming.put("password", "");

        Map<String, Object> merged = JsonSchemaValidator.mergeWriteOnlyFromExisting(
                LoginInputs.class,
                incoming,
                Map.of("username", "old", "password", "kept-secret")
        );

        assertEquals("alice", merged.get("username"));
        assertEquals("kept-secret", merged.get("password"));
    }

    @Test
    void mergeWriteOnlyFromExistingKeepsExplicitNewPassword() {
        Map<String, Object> merged = JsonSchemaValidator.mergeWriteOnlyFromExisting(
                LoginInputs.class,
                Map.of("username", "alice", "password", "new-secret"),
                Map.of("username", "old", "password", "kept-secret")
        );

        assertEquals("new-secret", merged.get("password"));
    }

    @Test
    void mergeWriteOnlyFromExistingFillsMissingKey() {
        Map<String, Object> merged = JsonSchemaValidator.mergeWriteOnlyFromExisting(
                LoginInputs.class,
                Map.of("username", "alice"),
                Map.of("username", "old", "password", "kept-secret")
        );

        assertEquals("kept-secret", merged.get("password"));
    }
}
