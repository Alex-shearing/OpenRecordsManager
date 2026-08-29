package com.openrecordsmanager.rest.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.record.RecordActionType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.api.user.UserActionType;
import com.openrecordsmanager.audit.AuditPolicyService;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ActionResponse(
        @NotNull ResourceIdentifier id,
        @NotBlank String name,
        @NotBlank String description,
        @NotNull InputFormSchema inputSchema,
        boolean requiresAuditComment
) {

    public static ActionResponse ofUser(ComponentCatalog catalog, UserActionType<?> action, AuditPolicyService auditPolicyService) {
        ResourceIdentifier id = catalog.getRegistry(ComponentTypes.USER_ACTION).getId(action)
                .orElseThrow();

        return new ActionResponse(
                id,
                action.getDisplayName(),
                action.getDescription(),
                InputFormSchema.from(action.getInputClass()),
                auditPolicyService.requiresComment(AuditEntityType.USER, AuditOperation.ACTION)
        );
    }

    public static ActionResponse ofRecord(ComponentCatalog catalog, RecordActionType<?> action, AuditPolicyService auditPolicyService) {
        ResourceIdentifier id = catalog.getRegistry(ComponentTypes.RECORD_ACTION).getId(action)
                .orElseThrow();

        return new ActionResponse(
                id,
                action.getDisplayName(),
                action.getDescription(),
                InputFormSchema.from(action.getInputClass()),
                auditPolicyService.requiresComment(AuditEntityType.RECORD, AuditOperation.ACTION)
        );
    }
}
