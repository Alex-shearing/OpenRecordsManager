package com.openrecordsmanager.api.auth;

import java.util.Map;
import java.util.UUID;

/**
 * Server-side pending state for a redirect-based login, keyed by OAuth {@code state}.
 */
public record PendingRedirectAuth(
        UUID providerId,
        String state,
        Map<String, String> attributes
) {
}
