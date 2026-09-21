package com.openrecordsmanager.api.auth;

import java.net.URI;
import java.util.Map;

/**
 * Result of starting a redirect-based authentication flow.
 *
 * @param redirectUri where the browser should be sent (typically the IdP authorize endpoint)
 * @param state       opaque CSRF state value that must be returned on the callback
 * @param attributes  provider-specific values to persist until callback (e.g. nonce, PKCE verifier)
 */
public record RedirectAuthChallenge(
        URI redirectUri,
        String state,
        Map<String, String> attributes
) {
}
