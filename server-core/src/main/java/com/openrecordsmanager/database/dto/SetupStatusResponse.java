package com.openrecordsmanager.database.dto;

import com.openrecordsmanager.database.schema.SchemaMigrationState;
import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;

import java.util.List;

public record SetupStatusResponse(
        @NotBlank SchemaMigrationState.Status state,
        @Nullable String currentVersion,
        @NotBlank List<String> pendingMigrations,
        @Nullable String message
) {
}
