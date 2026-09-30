package com.openrecordsmanager.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.networknt.schema.Schema;
import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

public record InputFormSchema(
        @NotBlank String type,
        @NotBlank boolean additionalProperties,
        @Nullable Map<String, InputFormSchemaField> properties,
        @Nullable List<String> required
) {

    public static InputFormSchema from(Schema schema) {
        return JsonSchemaValidator.MAPPER.convertValue(schema.getSchemaNode(), InputFormSchema.class);
    }

    public static InputFormSchema from(Class<? extends Record> recordClass) {
        return from(JsonSchemaValidator.getSchema(recordClass));
    }

    public record InputFormSchemaField(
            @NotBlank String type,
            @NotBlank String title,
            @Nullable String description,
            @Nullable Boolean writeOnly,
            @Nullable String format,
            @Nullable Integer minLength,
            @Nullable Integer maxLength,
            @Nullable String pattern,
            @Nullable String contentEncoding,
            @Nullable @JsonProperty("enum") List<String> enumValues
    ) {
    }
}
