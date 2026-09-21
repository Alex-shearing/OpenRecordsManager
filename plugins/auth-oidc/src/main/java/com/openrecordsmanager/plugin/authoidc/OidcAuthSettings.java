package com.openrecordsmanager.plugin.authoidc;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Persisted OIDC provider settings. {@code secret} is write-only in API responses.
 */
public record OidcAuthSettings(
        @Schema(title = "Client ID")
        @NotBlank String clientId,

        @Schema(title = "Client Secret", format = "password", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank String secret,

        @Schema(title = "Issuer URI", description = "OpenID Connect issuer URL")
        @NotBlank String uri,

        @Schema(title = "Scope", description = "Space-separated scopes; openid is always included", defaultValue = "openid profile email")
        @NotBlank String scope,

        @Schema(title = "Username claim", description = "ID token claim used as ORM username", defaultValue = "preferred_username")
        @NotBlank String usernameClaim
) {
}
