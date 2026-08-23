package com.openrecordsmanager.action.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.action.RecordActionType;
import com.openrecordsmanager.api.action.UserActionType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.rest.InputFormSchema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ActionResponse(
        @NotNull ResourceIdentifier id,
        @NotBlank String name,
        @NotBlank String description,
        @NotNull InputFormSchema inputSchema
) {

    public static ActionResponse ofUser(ComponentCatalog catalog, UserActionType<?> action) {
        ResourceIdentifier id = catalog.getRegistry(ComponentTypes.USER_ACTION).getId(action)
                .orElseThrow();

        return new ActionResponse(
                id,
                action.getDisplayName(),
                action.getDescription(),
                InputFormSchema.from(action.getInputClass())
        );
    }

    public static ActionResponse ofRecord(ComponentCatalog catalog, RecordActionType<?> action) {
        ResourceIdentifier id = catalog.getRegistry(ComponentTypes.RECORD_ACTION).getId(action)
                .orElseThrow();

        return new ActionResponse(
                id,
                action.getDisplayName(),
                action.getDescription(),
                InputFormSchema.from(action.getInputClass())
        );
    }
}
