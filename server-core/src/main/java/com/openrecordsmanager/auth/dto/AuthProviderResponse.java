package com.openrecordsmanager.auth.dto;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.auth.AuthProviderType;
import com.openrecordsmanager.api.auth.InputAuthProviderType;
import com.openrecordsmanager.auth.entity.AuthProvider;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.plugin.registry.mapper.ComponentReferenceSerializer;
import com.openrecordsmanager.rest.dto.InputFormSchema;
import com.openrecordsmanager.rest.errors.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.StringToClassMapItem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.annotation.JsonSerialize;

import java.util.Map;
import java.util.UUID;

public record AuthProviderResponse(
        @NotBlank UUID id,
        @NotBlank String name,
        @JsonSerialize(using = ComponentReferenceSerializer.class)
        @Schema(
                type = "object",
                implementation = Map.class,
                properties = {
                        @StringToClassMapItem(key = "id", value = String.class),
                        @StringToClassMapItem(key = "type", value = String.class),
                },
                requiredProperties = {"id", "type"}
        )
        @NotNull ComponentReference<?> type,
        InputFormSchema loginSchema
) {
    public static AuthProviderResponse of(ComponentCatalog catalog, AuthProvider provider) {
        AuthProviderType type = provider.getProviderType().getComponent(catalog)
                .orElseThrow(() -> new ResourceNotFoundException(
                        provider.getProviderType().getType(),
                        provider.getProviderType().getId(catalog).orElseThrow())
                );
        InputFormSchema schema = type instanceof InputAuthProviderType<?> inputType
                ? InputFormSchema.from(inputType.getInputClass())
                : null;

        return new AuthProviderResponse(
                provider.getId(),
                provider.getName(),
                provider.getProviderType(),
                schema
        );
    }
}
