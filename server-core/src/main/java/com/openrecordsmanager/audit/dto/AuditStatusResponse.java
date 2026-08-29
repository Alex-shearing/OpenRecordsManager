package com.openrecordsmanager.audit.dto;

import java.time.Instant;

public record AuditStatusResponse(
        boolean primaryWritable,
        int pendingSpoolCount,
        Instant lastProbeAt,
        Instant lastSuccessfulWriteAt,
        Instant lastDrainAttemptAt,
        Instant lastSuccessfulDrainAt
) {
}
