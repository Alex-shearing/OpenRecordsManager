package com.openrecordsmanager.auth.dto;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.auth.InputAuthProviderType;
import com.openrecordsmanager.api.schema.InputFormSchema;
import com.openrecordsmanager.auth.entity.AuthProvider;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.plugin.registry.mapper.ComponentReferenceSerializer;
import io.swagger.v3.oas.annotations.StringToClassMapItem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.annotation.JsonSerialize;

import java.util.Map;
import java.util.UUID;

public record AuthProviderListResponse(
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
        @Nullable @Schema(implementation = InputFormSchema.class) InputFormSchema loginSchema
) {
    public static AuthProviderListResponse of(AuthProvider provider, ComponentCatalog catalog) {
        InputFormSchema loginSchema = provider.getProviderType().getComponent(catalog)
                .filter(InputAuthProviderType.class::isInstance)
                .map(InputAuthProviderType.class::cast)
                .map(InputAuthProviderType::getLoginInputSchema)
                .map(InputFormSchema::from)
                .orElse(null);

        return new AuthProviderListResponse(provider.getId(), provider.getName(), provider.getProviderType(), loginSchema);
    }
}
