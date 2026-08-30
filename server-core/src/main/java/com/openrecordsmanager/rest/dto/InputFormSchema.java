package com.openrecordsmanager.rest.dto;

import com.networknt.schema.Schema;
import com.openrecordsmanager.api.schema.JsonSchemaValidator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record InputFormSchema(
        @NotBlank String type,
        @NotBlank boolean additionalProperties,
        @NotNull Map<String, InputFormSchemaField> properties,
        @NotNull List<String> required
) {

    public static InputFormSchema from(Schema schema) {
        return JsonSchemaValidator.MAPPER.convertValue(schema.getSchemaNode(), InputFormSchema.class);
    }

    public static InputFormSchema from(Class<? extends Record> recordClass) {
        return from(JsonSchemaValidator.getSchema(recordClass));
    }

}
