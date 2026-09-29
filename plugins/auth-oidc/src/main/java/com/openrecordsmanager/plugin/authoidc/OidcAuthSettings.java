package com.openrecordsmanager.plugin.authoidc;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Persisted OIDC provider settings. {@code secret} is write-only in API responses.
 */
public record OidcAuthSettings(
        @Schema(title = "redirect_auth_provider.auth_oidc.oidc_auth.schema.clientId.title")
        @NotBlank String clientId,

        @Schema(
                title = "redirect_auth_provider.auth_oidc.oidc_auth.schema.secret.title",
                format = "password",
                accessMode = Schema.AccessMode.WRITE_ONLY
        )
        @NotBlank String secret,

        @Schema(
                title = "redirect_auth_provider.auth_oidc.oidc_auth.schema.uri.title",
                description = "redirect_auth_provider.auth_oidc.oidc_auth.schema.uri.description"
        )
        @NotBlank String uri,

        @Schema(
                title = "redirect_auth_provider.auth_oidc.oidc_auth.schema.scope.title",
                description = "redirect_auth_provider.auth_oidc.oidc_auth.schema.scope.description",
                defaultValue = "openid profile email"
        )
        @NotBlank String scope,

        @Schema(
                title = "redirect_auth_provider.auth_oidc.oidc_auth.schema.usernameClaim.title",
                description = "redirect_auth_provider.auth_oidc.oidc_auth.schema.usernameClaim.description",
                defaultValue = "preferred_username"
        )
        @NotBlank String usernameClaim
) {
}
