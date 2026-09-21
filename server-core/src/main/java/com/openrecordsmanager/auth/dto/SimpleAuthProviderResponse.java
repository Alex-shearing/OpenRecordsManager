package com.openrecordsmanager.auth.dto;

import com.openrecordsmanager.api.auth.AuthProviderType;
import com.openrecordsmanager.api.auth.InputAuthProviderType;
import com.openrecordsmanager.auth.entity.AuthProvider;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.rest.dto.ComponentReferenceDto;
import com.openrecordsmanager.rest.dto.InputFormSchema;
import com.openrecordsmanager.rest.errors.ResourceNotFoundException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Public auth-provider view for unauthenticated clients.
 */
public record SimpleAuthProviderResponse(
        @NotBlank UUID id,
        @NotBlank String name,
        @NotNull ComponentReferenceDto type,
        @Nullable InputFormSchema loginSchema
) {
    public static SimpleAuthProviderResponse of(ComponentCatalog catalog, AuthProvider provider) {
        AuthProviderType<?> type = provider.getProviderType().getComponent(catalog)
                .orElseThrow(() -> new ResourceNotFoundException(
                        provider.getProviderType().getType(),
                        provider.getProviderType().getId(catalog).orElseThrow())
                );
        InputFormSchema schema = type instanceof InputAuthProviderType<?, ?> inputType
                ? InputFormSchema.from(inputType.getInputClass())
                : null;

        return new SimpleAuthProviderResponse(
                provider.getId(),
                provider.getName(),
                ComponentReferenceDto.of(catalog, provider.getProviderType()),
                schema
        );
    }
}
