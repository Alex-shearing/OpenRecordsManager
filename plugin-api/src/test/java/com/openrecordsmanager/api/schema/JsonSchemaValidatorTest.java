package com.openrecordsmanager.api.schema;

import com.openrecordsmanager.api.errors.InputValidationException;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonSchemaValidatorTest {

    public record LoginInputs(
            @Schema(title = "Username") @NotBlank String username,
            @Schema(title = "Password", format = "password", accessMode = Schema.AccessMode.WRITE_ONLY)
            @NotBlank String password
    ) {
    }

    public record ExplicitWriteOnlyInputs(
            @Schema(title = "Name") @NotBlank String name,
            @Schema(title = "Token", accessMode = Schema.AccessMode.WRITE_ONLY)
            @NotBlank String token
    ) {
    }

    public record SecretKeyInputs(
            @Schema(title = "Label") @NotBlank String label,
            @Schema(
                    title = "Secret Key",
                    type = "string",
                    format = "byte",
                    accessMode = Schema.AccessMode.WRITE_ONLY
            )
            byte[] secretKey
    ) {
    }

    @Test
    void generatesJsonSchemaFromRecord() {
        JsonNode schema = JsonSchemaValidator.getSchema(LoginInputs.class).getSchemaNode();

        assertEquals("object", schema.get("type").stringValue());
        assertFalse(schema.get("additionalProperties").booleanValue());
        assertEquals("Username", schema.get("properties").get("username").get("title").stringValue());
        assertTrue(schema.get("properties").get("username").get("minLength").intValue() >= 1);
        assertTrue(schema.get("properties").get("password").get("writeOnly").booleanValue());
        assertEquals("password", schema.get("properties").get("password").get("format").stringValue());

        JsonNode required = schema.get("required");
        assertEquals(2, required.size());
        assertTrue(required.toString().contains("username"));
        assertTrue(required.toString().contains("password"));
    }

    @Test
    void stripsSwaggerDefaultSentinel() {
        record Bare(@Schema(title = "Name") String name) {
        }

        JsonNode schema = JsonSchemaValidator.getSchemaNode(Bare.class);
        assertFalse(schema.get("properties").get("name").has("default"));
    }

    @Test
    void explicitWriteOnlySetsSchemaFlag() {
        JsonNode schema = JsonSchemaValidator.getSchema(ExplicitWriteOnlyInputs.class).getSchemaNode();
        assertTrue(schema.get("properties").get("token").get("writeOnly").booleanValue());
        assertNull(schema.get("properties").get("name").get("writeOnly"));
    }

    enum SampleEnum {ALPHA, BETA}

    public record EnumInputs(@Schema(title = "Mode") SampleEnum mode) {
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
    void mergeFromExistingFillsBlankAndMissingValues() {
        Map<String, Object> incoming = new HashMap<>();
        incoming.put("username", "");
        incoming.put("password", "");

        Map<String, Object> merged = JsonSchemaValidator.mergeFromExisting(
                incoming,
                Map.of("username", "kept-user", "password", "kept-secret")
        );

        assertEquals("kept-user", merged.get("username"));
        assertEquals("kept-secret", merged.get("password"));
    }

    @Test
    void mergeFromExistingKeepsExplicitNewValues() {
        Map<String, Object> merged = JsonSchemaValidator.mergeFromExisting(
                Map.of("username", "alice", "password", "new-secret"),
                Map.of("username", "old", "password", "kept-secret")
        );

        assertEquals("alice", merged.get("username"));
        assertEquals("new-secret", merged.get("password"));
    }

    @Test
    void mergeFromExistingFillsMissingKey() {
        Map<String, Object> merged = JsonSchemaValidator.mergeFromExisting(
                Map.of("username", "alice"),
                Map.of("username", "old", "password", "kept-secret")
        );

        assertEquals("alice", merged.get("username"));
        assertEquals("kept-secret", merged.get("password"));
    }
}
