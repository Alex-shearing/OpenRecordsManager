package com.openrecordsmanager.auth;

import com.openrecordsmanager.api.auth.PendingRedirectAuth;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.config.ConfigService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stores pending redirect-auth state in a short-lived signed HttpOnly cookie.
 */
@Component
public class PendingRedirectAuthCookie {

    public static final String COOKIE_NAME = "ORM-Redirect-Auth";
    public static final String CLAIM_STATE = "state";
    public static final String CLAIM_ATTRS = "attrs";
    public static final String CLAIM_RETURN_TO = "return_to";
    public static final String CLAIM_TYPE = "token_type";
    public static final String TOKEN_TYPE = "redirect_auth";

    private static final long TTL_SECONDS = 600;
    private static final String COOKIE_PATH = "/api/auth";

    private final SecretKey signingKey;
    private final HttpOnlyCookies cookies;

    public PendingRedirectAuthCookie(ConfigService config, HttpOnlyCookies cookies) {
        String key = config.getOrThrow(BuiltinConfigs.JWT_SIGNING_KEY);
        this.signingKey = Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8));
        this.cookies = cookies;
    }

    public void store(
            HttpServletResponse response,
            PendingRedirectAuth pending,
            @Nullable String returnTo
    ) {
        Instant now = Instant.now();
        Instant expires = now.plusSeconds(TTL_SECONDS);

        JwtBuilder builder = Jwts.builder()
                .subject(pending.providerId().toString())
                .claim(CLAIM_TYPE, TOKEN_TYPE)
                .claim(CLAIM_STATE, pending.state())
                .claim(CLAIM_ATTRS, pending.attributes())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expires))
                .signWith(this.signingKey);

        if (returnTo != null) {
            builder.claim(CLAIM_RETURN_TO, returnTo);
        }

        this.cookies.write(response, COOKIE_NAME, builder.compact(), TTL_SECONDS, COOKIE_PATH);
    }

    public @Nullable Loaded take(HttpServletRequest request, HttpServletResponse response, UUID expectedProviderId) {
        String raw = this.cookies.read(request, COOKIE_NAME);

        this.cookies.clear(response, COOKIE_NAME, COOKIE_PATH);

        if (raw == null) {
            return null;
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(this.signingKey)
                    .build()
                    .parseSignedClaims(raw)
                    .getPayload();

            if (!TOKEN_TYPE.equals(claims.get(CLAIM_TYPE, String.class))) {
                return null;
            }

            UUID providerId = UUID.fromString(claims.getSubject());
            if (!providerId.equals(expectedProviderId)) {
                return null;
            }

            String state = claims.get(CLAIM_STATE, String.class);
            if (StringUtils.isBlank(state)) {
                return null;
            }

            @SuppressWarnings("unchecked")
            Map<@Nullable String, @Nullable Object> rawAttrs = claims.get(CLAIM_ATTRS, Map.class);
            Map<String, String> attrs = new HashMap<>();
            if (rawAttrs != null) {
                rawAttrs.forEach((k, v) -> {
                    if (k != null && v != null) {
                        attrs.put(k, v.toString());
                    }
                });
            }

            String returnTo = claims.get(CLAIM_RETURN_TO, String.class);
            return new Loaded(new PendingRedirectAuth(providerId, state, Map.copyOf(attrs)), returnTo);
        } catch (Exception e) {
            return null;
        }
    }

    public void clear(HttpServletResponse response) {
        this.cookies.clear(response, COOKIE_NAME, COOKIE_PATH);
    }

    public record Loaded(PendingRedirectAuth pending, @Nullable String returnTo) {
    }
}
