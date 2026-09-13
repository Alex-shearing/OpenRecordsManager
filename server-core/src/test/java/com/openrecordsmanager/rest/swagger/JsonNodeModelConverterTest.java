package com.openrecordsmanager.rest.swagger;

import com.openrecordsmanager.user.dto.UserResponse;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springdoc.core.utils.SpringDocUtils;
import tools.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JsonNodeModelConverterTest {

    @BeforeAll
    static void registerConverter() {
        SpringDocUtils.getConfig().replaceWithSchema(JsonNode.class, JsonNodeModelConverter.freeFormJsonSchema());
        ModelConverters.getInstance().addConverter(new JsonNodeModelConverter());
    }

    @Test
    void jsonNodeResolvesAsFreeFormNotBeanProperties() {
        ResolvedSchema resolved = ModelConverters.getInstance()
                .resolveAsResolvedSchema(new AnnotatedType(JsonNode.class));

        assertNotNull(resolved.schema);
        assertNull(resolved.schema.getProperties(), "JsonNode must not expose bean getters");
        assertFalse(hasJsonNodeBeanNoise(resolved.schema));
        assertFalse(isObjectOnly(resolved.schema), "JsonNode must not collapse to type=object");
    }

    @Test
    void freeFormSchemaSurvivesOpenApi31HandleSchemaTypes() {
        Schema<?> schema = JsonNodeModelConverter.freeFormJsonSchema();
        SpringDocUtils.handleSchemaTypes(schema);

        assertFalse(isObjectOnly(schema), "springdoc must not rewrite free-form JsonNode to type=object");
        assertNotNull(schema.getOneOf());
    }

    @Test
    void userResponsePropertiesMapIsFreeFormAdditionalProperties() {
        ResolvedSchema resolved = ModelConverters.getInstance()
                .resolveAsResolvedSchema(new AnnotatedType(UserResponse.class).resolveAsRef(false));

        Schema<?> userSchema = resolved.schema;
        assertNotNull(userSchema);
        Map<String, Schema> properties = userSchema.getProperties();
        assertNotNull(properties);
        Schema<?> props = properties.get("properties");
        assertNotNull(props);

        Object additional = props.getAdditionalProperties();
        assertNotNull(additional, "properties map should declare additionalProperties");
        if (additional instanceof Schema<?> valueSchema) {
            assertFalse(hasJsonNodeBeanNoise(valueSchema));
            assertFalse(isObjectOnly(valueSchema), "map values must stay free-form JSON, not object");
        }
    }

    private static boolean isObjectOnly(Schema<?> schema) {
        if (schema.getOneOf() != null || schema.getAnyOf() != null || schema.getAllOf() != null) {
            return false;
        }
        Set<String> types = schema.getTypes();
        if (types != null) {
            return types.equals(Set.of("object"));
        }
        return "object".equals(schema.getType());
    }

    private static boolean hasJsonNodeBeanNoise(Schema<?> schema) {
        Map<String, Schema> properties = schema.getProperties();
        return properties != null && properties.containsKey("array") && properties.containsKey("null");
    }
}
