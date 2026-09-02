package com.openrecordsmanager.audit.dto;

import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

public record AuditStatusResponse(
        @NotBlank boolean auditEnabled,
        @Nullable String auditDisabledReason,
        @NotBlank boolean primaryWritable,
        @NotBlank int pendingSpoolCount,
        @NotBlank boolean archiveEnabled,
        @NotBlank long drainIntervalSeconds,
        @NotBlank Instant lastProbeAt,
        @NotBlank Instant lastSuccessfulWriteAt,
        @NotBlank Instant lastDrainAttemptAt,
        @NotBlank Instant lastSuccessfulDrainAt
) {
}
