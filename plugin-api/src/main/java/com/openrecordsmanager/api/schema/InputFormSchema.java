package com.openrecordsmanager.api.schema;

import com.networknt.schema.Schema;
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
        return RecordInputs.MAPPER.convertValue(schema.getSchemaNode(), InputFormSchema.class);
    }

    public static InputFormSchema from(Class<? extends Record> recordClass) {
        return from(JsonSchemaValidator.getSchema(recordClass));
    }
}
