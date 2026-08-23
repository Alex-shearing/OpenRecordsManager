package com.openrecordsmanager.rest;

import com.networknt.schema.Schema;
import com.openrecordsmanager.api.schema.JsonSchemaValidator;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

public record InputFormSchema(
        @Nullable String type,
        @Nullable Boolean additionalProperties,
        @Nullable Map<String, InputFormSchemaField> properties,
        @Nullable List<String> required
) {

    public static InputFormSchema from(Schema schema) {
        return JsonSchemaValidator.MAPPER.convertValue(schema.getSchemaNode(), InputFormSchema.class);
    }

    public static InputFormSchema from(Class<? extends Record> recordClass) {
        return from(JsonSchemaValidator.getSchema(recordClass));
    }

}
