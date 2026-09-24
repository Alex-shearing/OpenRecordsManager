package com.openrecordsmanager.audit.dto;

import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.audit.persistence.AuditPolicyEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record AuditPolicyResponse(
        @NotBlank AuditEntityType entityType,
        @NotBlank @Schema(enumAsRef = true) AuditOperation operation,
        @NotBlank boolean enabled,
        @NotBlank boolean requiresComment,
        @NotBlank String displayName,
        @NotBlank String description,
        @NotNull Instant dateModified
) {
    public static AuditPolicyResponse of(AuditPolicyEntity entity) {
        return new AuditPolicyResponse(
                entity.entityType(),
                entity.operation(),
                entity.isEnabled(),
                entity.isRequiresComment(),
                entity.getDisplayName(),
                entity.getDescription() == null ? "" : entity.getDescription(),
                entity.getDateModified()
        );
    }
}
