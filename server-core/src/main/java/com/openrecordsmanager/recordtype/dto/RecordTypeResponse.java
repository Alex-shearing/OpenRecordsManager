package com.openrecordsmanager.recordtype.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.recordtype.SecurityFilterUsage;
import com.openrecordsmanager.recordtype.RecordType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.stream.Collectors;

public record RecordTypeResponse(
        @NotBlank ResourceIdentifier id,
        @NotBlank String name,
        @NotBlank String description,
        @Nullable String securityFilter,
        @NotNull SecurityFilterUsage securityFilterUsage,
        @Nullable Set<String> contentTypes,
        @NotNull Set<RecordTypePropertyResponse> properties
) {
    public static RecordTypeResponse of(RecordType recordType) {
        return new RecordTypeResponse(
                recordType.getId(),
                recordType.getName(),
                recordType.getDescription(),
                recordType.getSecurityFilter(),
                recordType.getSecurityFilterUsage(),
                recordType.getContentTypes(),
                recordType.getProperties().stream()
                        .map(RecordTypePropertyResponse::of)
                        .collect(Collectors.toSet())
        );
    }
}
