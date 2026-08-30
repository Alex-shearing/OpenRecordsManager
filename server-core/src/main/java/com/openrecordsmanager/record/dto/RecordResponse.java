package com.openrecordsmanager.record.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.record.Record;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record RecordResponse(
        @NotBlank UUID id,
        @NotBlank ResourceIdentifier type,
        @NotNull Map<String, Object> properties,
        @NotNull List<String> revisions
) {

    public static RecordResponse of(Record record) {
        return new RecordResponse(
                record.getId(),
                record.getType().id,
                record.toPropertyMap(true),
                record.getRevisionList()
        );
    }
}
