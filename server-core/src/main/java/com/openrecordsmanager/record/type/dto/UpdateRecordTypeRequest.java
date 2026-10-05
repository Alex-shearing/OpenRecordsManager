package com.openrecordsmanager.record.type.dto;

import com.openrecordsmanager.api.template.recordtype.SecurityFilterUsage;
import com.openrecordsmanager.property.dto.TypePropertyAssignment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

public record UpdateRecordTypeRequest(
        @Nullable Set<String> contentTypes,
        @Nullable String securityFilter,
        @NotNull SecurityFilterUsage securityFilterUsage,
        @NotNull @Valid List<TypePropertyAssignment> properties
) {
}
