package com.openrecordsmanager.auth.dto;

import com.openrecordsmanager.auth.entity.AuthProvider;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.rest.dto.ComponentReferenceDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuthProviderResponse(
        @NotBlank UUID id,
        @NotBlank String name,
        @NotNull ComponentReferenceDto type,
        @NotBlank boolean enabled,
        @Schema(description = "Provider settings with write-only fields omitted", additionalProperties = Schema.AdditionalPropertiesValue.TRUE)
        @Nullable Map<String, ?> settings,
        @NotNull Instant dateCreated,
        @NotNull Instant dateModified
) {
    public static AuthProviderResponse of(ComponentCatalog catalog, AuthProvider provider) {
        return new AuthProviderResponse(
                provider.getId(),
                provider.getName(),
                ComponentReferenceDto.of(catalog, provider.getProviderType()),
                provider.isEnabled(),
                provider.getSettings(catalog),
                provider.getDateCreated(),
                provider.getDateModified()
        );
    }
}
