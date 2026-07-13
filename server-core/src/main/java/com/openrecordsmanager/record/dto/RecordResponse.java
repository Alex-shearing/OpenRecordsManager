package com.openrecordsmanager.record.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.record.Record;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record RecordResponse(
        @NotBlank UUID id,
        @NotBlank String title,
        @NotBlank ResourceIdentifier type,
        @NotNull Map<String, Object> properties,
        @NotNull Set<String> revisions
) {

    public static RecordResponse from(Record record) {
        return new RecordResponse(
                record.id,
                record.title,
                record.getType().id,
                record.toPropertyMap(),
                record.revisions.stream()
                        .map(recordRevision -> recordRevision.version)
                        .collect(Collectors.toSet())
        );
    }
}
