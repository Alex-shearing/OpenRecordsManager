package com.openrecordsmanager.record.type.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.recordtype.SecurityFilterUsage;
import com.openrecordsmanager.record.type.RecordType;
import com.openrecordsmanager.template.dto.ManagedObjectTypePropertyResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Set;

public record RecordTypeResponse(
        @NotBlank ResourceIdentifier id,
        @Nullable String securityFilter,
        @NotNull SecurityFilterUsage securityFilterUsage,
        @Nullable Set<String> contentTypes,
        boolean abstractType,
        @Nullable ResourceIdentifier baseTypeId,
        @NotNull Set<ManagedObjectTypePropertyResponse> declaredProperties,
        @NotNull Set<ManagedObjectTypePropertyResponse> effectiveProperties,
        @NotNull Instant dateCreated,
        @NotNull Instant dateModified
) {
    public static RecordTypeResponse of(RecordType recordType) {
        return new RecordTypeResponse(
                recordType.getId(),
                recordType.getSecurityFilter(),
                recordType.getSecurityFilterUsage(),
                recordType.getContentTypes(),
                recordType.isAbstract(),
                recordType.getBaseTypeId(),
                ManagedObjectTypePropertyResponse.ofAll(recordType.getDeclaredProperties()),
                ManagedObjectTypePropertyResponse.ofAll(recordType.getEffectiveProperties()),
                recordType.getDateCreated(),
                recordType.getDateModified()
        );
    }
}
