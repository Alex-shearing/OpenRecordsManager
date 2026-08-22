package com.openrecordsmanager.api.schema;

import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import com.openrecordsmanager.api.errors.InputValidationException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class JsonSchemaValidator {
    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final SchemaRegistry SCHEMA_REGISTRY =
            SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);

    private JsonSchemaValidator() {
    }

    public static Schema getSchema(Class<? extends Record> recordClass) {
        return SCHEMA_REGISTRY.getSchema(jsonSchemaFromClass(recordClass));
    }

    public static Map<String, Object> validate(Class<? extends Record> recordClass, Map<String, ?> inputs) throws InputValidationException {
        Schema compiledSchema = getSchema(recordClass);
        JsonNode inputNode = MAPPER.valueToTree(inputs);

        List<Error> errors = compiledSchema.validate(inputNode, executionContext ->
                executionContext.executionConfig(config -> config.formatAssertionsEnabled(true)));
        if (!errors.isEmpty()) {
            Map<String, String> fieldErrors = new HashMap<>();
            for (Error error : errors) {
                String field = fieldName(error);
                fieldErrors.putIfAbsent(field, error.getMessage());
            }

            throw new InputValidationException(fieldErrors);
        }

        Map<String, Object> normalized = new HashMap<>();
        inputNode.properties().forEach(entry -> {
            JsonNode value = entry.getValue();
            Object converted = MAPPER.convertValue(value, Object.class);
            if (converted instanceof String stringValue) {
                converted = stringValue.trim();
            }
            normalized.put(entry.getKey(), converted);
        });

        return normalized;
    }

    private static String fieldName(Error error) {
        String property = error.getProperty();
        if (property != null && !property.isEmpty()) {
            return property;
        }

        String location = error.getInstanceLocation().toString();
        if (location.isEmpty() || "$".equals(location)) {
            return "_form";
        }

        String field = location;
        if (field.startsWith("$.")) {
            field = field.substring(2);
        } else if (field.startsWith("#/")) {
            field = field.substring(2);
        } else if (field.startsWith("/")) {
            field = field.substring(1);
        }

        int nested = field.indexOf('/');
        if (nested >= 0) {
            field = field.substring(0, nested);
        }

        return field.isEmpty() ? "_form" : field;
    }

    private static JsonNode jsonSchemaFromClass(Class<? extends Record> recordClass) {
        ObjectNode schema = RecordInputs.MAPPER.createObjectNode();
        schema.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        schema.put("type", "object");
        schema.put("additionalProperties", false);

        ObjectNode properties = schema.putObject("properties");
        List<String> required = new ArrayList<>();

        for (RecordComponent component : recordClass.getRecordComponents()) {
            SchemaField field = component.getAnnotation(SchemaField.class);
            if (field == null) {
                throw new IllegalArgumentException(
                        "Record component '" + component.getName() + "' on " + recordClass.getName()
                                + " must be annotated with @SchemaField");
            }

            ObjectNode property = properties.putObject(component.getName());
            applyFieldType(property, component.getType(), field);

            if (!field.description().isEmpty()) {
                property.put("description", field.description());
            }

            if (isStringLike(component.getType()) || component.getType().isEnum() || component.getType() == byte[].class) {
                if (field.minLength() >= 0) {
                    property.put("minLength", field.minLength());
                }

                if (field.maxLength() >= 0) {
                    property.put("maxLength", field.maxLength());
                }

                if (!field.pattern().isEmpty()) {
                    property.put("pattern", field.pattern());
                }
            }

            if (field.required()) {
                required.add(component.getName());
            }
        }

        if (!required.isEmpty()) {
            ArrayNode requiredNode = schema.putArray("required");
            required.forEach(requiredNode::add);
        }

        return schema;
    }

    private static void applyFieldType(ObjectNode property, Class<?> componentType, SchemaField field) {
        property.put("title", field.title());

        if (componentType.isEnum()) {
            property.put("type", "string");
            ArrayNode enumValues = property.putArray("enum");
            for (Object constant : componentType.getEnumConstants()) {
                enumValues.add(((Enum<?>) constant).name());
            }
            return;
        }

        if (componentType == byte[].class) {
            property.put("type", "string");
            property.put("contentEncoding", "base64");
            if (field.format() == SchemaFieldFormat.PASSWORD) {
                property.put("writeOnly", true);
            }
            return;
        }

        if (componentType == boolean.class || componentType == Boolean.class) {
            property.put("type", "boolean");
            return;
        }

        if (componentType == int.class || componentType == Integer.class
                || componentType == long.class || componentType == Long.class) {
            property.put("type", "integer");
            return;
        }

        if (componentType == double.class || componentType == Double.class
                || componentType == float.class || componentType == Float.class) {
            property.put("type", "number");
            return;
        }

        property.put("type", "string");

        if (field.format() == SchemaFieldFormat.PASSWORD) {
            property.put("writeOnly", true);
        } else if (field.format() == SchemaFieldFormat.EMAIL) {
            property.put("format", "email");
        }
    }

    private static boolean isStringLike(Class<?> componentType) {
        return componentType == String.class || componentType == char.class || componentType == Character.class;
    }
}
