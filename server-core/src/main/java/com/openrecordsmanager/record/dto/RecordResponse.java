package com.openrecordsmanager.record.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.record.Record;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record RecordResponse(
        @NotBlank UUID id,
        @NotBlank ResourceIdentifier type,
        @NotNull Map<String, @Nullable JsonNode> properties,
        @NotNull List<String> revisions,
        @NotNull boolean canAccessRevisions
) {

    public static RecordResponse of(Record record, boolean canAccessRevisions) {
        return new RecordResponse(
                record.getId(),
                record.getType().getId(),
                record.toWireMap(),
                record.getRevisionList(),
                canAccessRevisions
        );
    }
}
