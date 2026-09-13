package com.openrecordsmanager.api.schema;

import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.github.victools.jsonschema.module.jackson.JacksonSchemaModule;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption;
import com.github.victools.jsonschema.module.swagger2.Swagger2Module;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.errors.InputValidationException;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.node.ObjectNode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class JsonSchemaValidator {
    public static final ObjectMapper MAPPER = JsonMapper.builder()
            // Template JSON may omit primitives that builders default (e.g. list entry index → 0).
            .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .addModule(new SimpleModule()
                    .addKeyDeserializer(ComponentReference.class, new ComponentReference.RefKeyDeserializer())
            )
            .build();

    private static final SchemaRegistry SCHEMA_REGISTRY =
            SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);

    private static final SchemaGenerator SCHEMA_GENERATOR = createSchemaGenerator();

    private static final ConcurrentHashMap<Class<? extends Record>, ObjectNode> GENERATED_SCHEMAS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<? extends Record>, Schema> COMPILED_SCHEMAS = new ConcurrentHashMap<>();

    private JsonSchemaValidator() {
    }

    private static SchemaGenerator createSchemaGenerator() {
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(
                MAPPER,
                SchemaVersion.DRAFT_2020_12,
                OptionPreset.PLAIN_JSON
        )
                .with(new JacksonSchemaModule(JacksonOption.RESPECT_JSONPROPERTY_REQUIRED))
                .with(new Swagger2Module())
                .with(new JakartaValidationModule(
                        JakartaValidationOption.NOT_NULLABLE_FIELD_IS_REQUIRED,
                        JakartaValidationOption.NOT_NULLABLE_METHOD_IS_REQUIRED,
                        JakartaValidationOption.INCLUDE_PATTERN_EXPRESSIONS
                ))
                .with(
                        Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT,
                        Option.FLATTENED_ENUMS,
                        Option.EXTRA_OPEN_API_FORMAT_VALUES,
                        Option.INLINE_ALL_SCHEMAS
                )
                .without(Option.SCHEMA_VERSION_INDICATOR);

        // Treat byte[] as OpenAPI "byte" (base64 string) for form schemas.
        configBuilder.forFields().withTargetTypeOverridesResolver(field -> {
            if (field.getType().getErasedType() == byte[].class) {
                return List.of(field.getContext().resolve(String.class));
            }
            return null;
        });
        configBuilder.forFields().withInstanceAttributeOverride((node, field, context) -> {
            if (field.getDeclaredType().getErasedType() == byte[].class
                    || field.getRawMember() != null && field.getRawMember().getType() == byte[].class) {
                node.put("type", "string");
                node.put("format", "byte");
                node.put("contentEncoding", "base64");
            }
            // Swagger @Schema defaultValue sentinel leaks into JSON Schema otherwise.
            if ("##default".equals(node.path("default").asString(null))) {
                node.remove("default");
            }
        });

        return new SchemaGenerator(configBuilder.build());
    }

    public static Schema getSchema(Class<? extends Record> recordClass) {
        return COMPILED_SCHEMAS.computeIfAbsent(recordClass, type ->
                SCHEMA_REGISTRY.getSchema(getSchemaNode(type))
        );
    }

    public static ObjectNode getSchemaNode(Class<? extends Record> recordClass) {
        return GENERATED_SCHEMAS.computeIfAbsent(recordClass, SCHEMA_GENERATOR::generateSchema);
    }

    public static Map<String, Object> validateAndSerialize(Class<? extends Record> recordClass, Object inputs) throws InputValidationException {
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

    /**
     * Converts the provided data into the provided record type.
     *
     * @param recordClass the record type to convert into
     * @param values      the input data
     * @return the record
     */
    public static <I extends Record> I toRecord(Class<I> recordClass, Map<String, ?> values) throws InputValidationException {
        Map<String, Object> validated = validateAndSerialize(recordClass, values);
        return MAPPER.convertValue(validated, recordClass);
    }

    public static Map<String, ?> serializeSettings(Record record) throws InputValidationException {
        return validateAndSerialize(record.getClass(), record);
    }

    /**
     * Like {@link #serializeSettings} but omits write-only fields so secrets are absent from API responses.
     */
    public static Map<String, ?> serializeSettingsForClient(Record record) throws InputValidationException {
        Map<String, Object> serialized = new LinkedHashMap<>(serializeSettings(record));
        for (String name : writeOnlyPropertyNames(record.getClass())) {
            serialized.remove(name);
        }
        return serialized;
    }

    /**
     * Copies values from {@code existing} into {@code incoming} when the client omitted them
     * or sent a blank value (empty string / empty byte array). Used on settings updates.
     */
    public static Map<String, Object> mergeFromExisting(
            Map<String, ?> incoming,
            Map<String, ?> existing
    ) {
        Map<String, Object> merged = new LinkedHashMap<>(incoming);
        if (existing.isEmpty()) {
            return merged;
        }

        for (Map.Entry<String, ?> entry : existing.entrySet()) {
            if (isBlankValue(merged.get(entry.getKey()))) {
                merged.put(entry.getKey(), entry.getValue());
            }
        }
        return merged;
    }

    private static List<String> writeOnlyPropertyNames(Class<? extends Record> recordClass) {
        JsonNode properties = getSchemaNode(recordClass).get("properties");
        if (properties != null && properties.isObject()) {
            List<String> fromSchema = new ArrayList<>();
            properties.properties().forEach(entry -> {
                if (entry.getValue().path("writeOnly").asBoolean(false)
                        || "password".equalsIgnoreCase(entry.getValue().path("format").asString(null))) {
                    fromSchema.add(entry.getKey());
                }
            });
            if (!fromSchema.isEmpty()) {
                return fromSchema;
            }
        }

        return List.of();
    }

    private static boolean isBlankValue(@Nullable Object value) {
        return switch (value) {
            case String stringValue -> stringValue.isBlank();
            case byte[] bytes -> bytes.length == 0;
            case List<?> list -> list.isEmpty();
            case null, default -> false;
        };
    }
}
