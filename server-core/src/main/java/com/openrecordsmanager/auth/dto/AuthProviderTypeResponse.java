package com.openrecordsmanager.auth.dto;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.auth.AuthProviderType;
import com.openrecordsmanager.api.auth.InputAuthProviderType;
import com.openrecordsmanager.api.auth.RedirectAuthProviderType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.rest.dto.ComponentReferenceDto;
import com.openrecordsmanager.rest.dto.InputFormSchema;
import com.openrecordsmanager.rest.errors.ResourceNotFoundException;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

public record AuthProviderTypeResponse(
        @NotNull ComponentReferenceDto type,
        @Nullable InputFormSchema settingsSchema
) {
    public static AuthProviderTypeResponse of(ComponentCatalog catalog, AuthProviderType<?> type) {
        return switch (type) {
            case InputAuthProviderType<?, ?> input -> ofInput(catalog, input);
            case RedirectAuthProviderType<?> redirect -> ofRedirect(catalog, redirect);
            default -> throw new IllegalArgumentException("Unexpected auth provider type: " + type.getClass());
        };
    }

    private static AuthProviderTypeResponse ofInput(ComponentCatalog catalog, InputAuthProviderType<?, ?> type) {
        ResourceIdentifier id = catalog.getRegistry(ComponentTypes.INPUT_AUTH_PROVIDER).getId(type)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.INPUT_AUTH_PROVIDER, type.getClass()));

        return new AuthProviderTypeResponse(
                new ComponentReferenceDto(id, ComponentTypes.INPUT_AUTH_PROVIDER.name()),
                InputFormSchema.from(type.getSettingsClass())
        );
    }

    private static AuthProviderTypeResponse ofRedirect(ComponentCatalog catalog, RedirectAuthProviderType<?> type) {
        ResourceIdentifier id = catalog.getRegistry(ComponentTypes.REDIRECT_AUTH_PROVIDER).getId(type)
                .orElseThrow(() -> new ResourceNotFoundException(ComponentTypes.REDIRECT_AUTH_PROVIDER, type.getClass()));

        return new AuthProviderTypeResponse(
                new ComponentReferenceDto(id, ComponentTypes.REDIRECT_AUTH_PROVIDER.name()),
                InputFormSchema.from(type.getSettingsClass())
        );
    }
}
