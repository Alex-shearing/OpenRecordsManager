package com.openrecordsmanager.recordtype.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openrecordsmanager.property.dto.ObjectPropertyResponse;
import com.openrecordsmanager.recordtype.RecordTypeProperty;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

public record RecordTypePropertyResponse(
        @NotNull ObjectPropertyResponse property,
        @JsonProperty("default") @Nullable Object defaultValue
) {
    public static RecordTypePropertyResponse of(RecordTypeProperty<?> property) {
        return new RecordTypePropertyResponse(
                ObjectPropertyResponse.of(property.property),
                property.getDefault()
        );
    }
}
