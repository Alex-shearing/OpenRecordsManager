package com.openrecordsmanager.record.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.record.Record;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record RecordResponse(
        @NotBlank UUID id,
        @NotBlank ResourceIdentifier type,
        @NotNull Map<String, JsonNode> properties,
        @NotNull List<String> revisions
) {

    public static RecordResponse of(Record record) {
        return new RecordResponse(
                record.getId(),
                record.getType().getId(),
                record.toWireMap(),
                record.getRevisionList()
        );
    }
}
