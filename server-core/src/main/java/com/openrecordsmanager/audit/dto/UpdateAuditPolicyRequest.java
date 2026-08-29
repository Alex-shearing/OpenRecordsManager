package com.openrecordsmanager.audit.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateAuditPolicyRequest(
        @NotNull Boolean enabled,
        @NotNull Boolean requiresComment
) {
}
