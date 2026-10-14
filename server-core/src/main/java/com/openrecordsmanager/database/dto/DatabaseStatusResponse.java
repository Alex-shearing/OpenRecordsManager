package com.openrecordsmanager.database.dto;

import com.openrecordsmanager.database.schema.SchemaMigrationState;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

public record DatabaseStatusResponse(
        @NotNull SchemaMigrationState.Status state,
        @Nullable String currentVersion,
        @NotNull List<String> pendingMigrations,
        @NotNull int migrationsApplied,
        @NotNull boolean writable
) {
}
