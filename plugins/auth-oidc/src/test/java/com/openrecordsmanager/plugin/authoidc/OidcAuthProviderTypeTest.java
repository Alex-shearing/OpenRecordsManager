package com.openrecordsmanager.plugin.authoidc;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.openrecordsmanager.api.auth.PendingRedirectAuth;
import com.openrecordsmanager.api.auth.RedirectAuthChallenge;
import com.openrecordsmanager.api.auth.UserAuthContext;
import com.openrecordsmanager.api.auth.UserAuthDetails;
import com.openrecordsmanager.api.template.property.ObjectPropertyTemplate;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class OidcAuthProviderTypeTest {

    private static final String CLIENT_ID = "orm-client";
    private static final String CLIENT_SECRET = "0123456789abcdef0123456789abcdef"; // 32 bytes

    private HttpServer server;
    private String issuer;
    private OidcAuthProviderType provider;
    private final AtomicReference<String> tokenResponse = new AtomicReference<>("{}");
    private final AtomicReference<String> lastTokenBody = new AtomicReference<>();

    @BeforeEach
    void setUp() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = this.server.getAddress().getPort();
        this.issuer = "http://127.0.0.1:" + port + "/";

        this.server.createContext("/.well-known/openid-configuration", exchange -> {
            String body = """
                    {
                      "issuer": "%s",
                      "authorization_endpoint": "%sauthorize",
                      "token_endpoint": "%stoken",
                      "jwks_uri": "%sjwks",
                      "response_types_supported": ["code"],
                      "subject_types_supported": ["public"],
                      "id_token_signing_alg_values_supported": ["HS256"]
                    }
                    """.formatted(this.issuer, this.issuer, this.issuer, this.issuer);
            writeJson(exchange, 200, body);
        });

        this.server.createContext("/token", exchange -> {
            this.lastTokenBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            writeJson(exchange, 200, this.tokenResponse.get());
        });

        this.server.start();
        this.provider = new OidcAuthProviderType();
    }

    @AfterEach
    void tearDown() {
        if (this.server != null) {
            this.server.stop(0);
        }
    }

    @Test
    void parseScopeEnsuresOpenId() {
        Scope scope = OidcAuthProviderType.parseScope("profile email");
        assertTrue(scope.toString().contains("openid"));
        assertTrue(scope.toString().contains("profile"));
    }

    @Test
    void resolveUsernameUsesConfiguredClaimThenFallbacks() throws Exception {
        Instant now = Instant.now();
        IDTokenClaimsSet claims = new IDTokenClaimsSet(new JWTClaimsSet.Builder()
                .issuer("https://idp.example.com/")
                .subject("sub-1")
                .audience("client")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(300)))
                .claim("preferred_username", "alice")
                .claim("email", "alice@example.com")
                .claim("custom_user", "custom-alice")
                .build());

        assertEquals("alice", OidcAuthProviderType.resolveUsername(claims, "preferred_username"));
        assertEquals("custom-alice", OidcAuthProviderType.resolveUsername(claims, "custom_user"));
        assertEquals("alice", OidcAuthProviderType.resolveUsername(claims, "missing_claim"));

        IDTokenClaimsSet subOnly = new IDTokenClaimsSet(new JWTClaimsSet.Builder()
                .issuer("https://idp.example.com/")
                .subject("sub-only")
                .audience("client")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(300)))
                .build());
        assertEquals("sub-only", OidcAuthProviderType.resolveUsername(subOnly, "preferred_username"));
    }

    @Test
    void beginIncludesStateNonceAndPkce() {
        RedirectAuthChallenge challenge = this.provider.beginUntyped(
                URI.create("http://localhost:8080/api/auth/callback/" + UUID.randomUUID()),
                Map.of(
                        "clientId", CLIENT_ID,
                        "secret", CLIENT_SECRET,
                        "uri", this.issuer,
                        "scope", "openid profile"
                )
        );

        assertNotNull(challenge.state());
        assertFalse(challenge.state().isBlank());
        assertTrue(challenge.attributes().containsKey(OidcAuthProviderType.ATTR_NONCE));
        assertTrue(challenge.attributes().containsKey(OidcAuthProviderType.ATTR_CODE_VERIFIER));
        String query = challenge.redirectUri().getQuery();
        assertTrue(query.contains("state=" + challenge.state()));
        assertTrue(query.contains("nonce="));
        assertTrue(query.contains("code_challenge="));
        assertTrue(query.contains("code_challenge_method=S256"));
    }

    @Test
    void completeRejectsStateMismatch() {
        UUID id = UUID.randomUUID();
        Map<String, ?> settings = Map.of(
                "clientId", CLIENT_ID,
                "secret", CLIENT_SECRET,
                "uri", this.issuer,
                "scope", "openid"
        );

        URI callback = URI.create("http://localhost:8080/api/auth/callback/" + id);
        RedirectAuthChallenge challenge = this.provider.beginUntyped(callback, settings);

        PendingRedirectAuth pending = new PendingRedirectAuth(
                id,
                challenge.state(),
                challenge.attributes()
        );

        URI forged = URI.create(callback + "?code=abc&state=wrong-state");
        assertNull(this.provider.completeUntyped(emptyContext(), forged, pending, settings));
    }

    @Test
    void completeHappyPathMapsPreferredUsername() throws Exception {
        UUID id = UUID.randomUUID();
        Map<String, ?> settings = Map.of(
                "clientId", CLIENT_ID,
                "secret", CLIENT_SECRET,
                "uri", this.issuer,
                "scope", "openid"
        );

        URI callback = URI.create("http://localhost:8080/api/auth/callback/" + id);
        RedirectAuthChallenge challenge = this.provider.beginUntyped(callback, settings);
        String nonce = challenge.attributes().get(OidcAuthProviderType.ATTR_NONCE);

        String idToken = signIdToken(nonce, "alice", "alice@example.com");
        this.tokenResponse.set("""
                {
                  "access_token": "access",
                  "token_type": "Bearer",
                  "id_token": "%s"
                }
                """.formatted(idToken));

        PendingRedirectAuth pending = new PendingRedirectAuth(
                id,
                challenge.state(),
                challenge.attributes()
        );

        URI fullCallback = URI.create(callback + "?code=auth-code&state=" + challenge.state());
        UserAuthDetails details = this.provider.completeUntyped(
                emptyContext(),
                fullCallback,
                pending,
                settings
        );

        assertNotNull(details);
        assertEquals("alice", details.username());
        assertEquals("alice@example.com", details.email());
        assertTrue(this.lastTokenBody.get().contains("code_verifier"));
    }

    @Test
    void completeFailsClosedOnTokenError() {
        UUID id = UUID.randomUUID();
        Map<String, ?> settings = Map.of(
                "clientId", CLIENT_ID,
                "secret", CLIENT_SECRET,
                "uri", this.issuer,
                "scope", "openid"
        );


        URI callback = URI.create("http://localhost:8080/api/auth/callback/" + id);
        RedirectAuthChallenge challenge = this.provider.beginUntyped(callback, settings);

        this.server.removeContext("/token");
        this.server.createContext("/token", exchange -> writeJson(exchange, 400, """
                {"error":"invalid_grant"}
                """));

        PendingRedirectAuth pending = new PendingRedirectAuth(
                id,
                challenge.state(),
                challenge.attributes()
        );

        URI fullCallback = URI.create(callback + "?code=auth-code&state=" + challenge.state());
        assertNull(this.provider.completeUntyped(emptyContext(), fullCallback, pending, settings));
    }

    private static UserAuthContext emptyContext() {
        return new UserAuthContext() {
            @Override
            public <T> @NonNull Optional<T> getUserProperty(
                    @NonNull String username,
                    @NonNull ObjectPropertyTemplate<T> property
            ) {
                return Optional.empty();
            }
        };
    }

    private String signIdToken(String nonce, String preferredUsername, String email) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(this.issuer)
                .subject("sub-" + preferredUsername)
                .audience(CLIENT_ID)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(300)))
                .claim("nonce", nonce)
                .claim("preferred_username", preferredUsername)
                .claim("email", email)
                .build();

        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(CLIENT_SECRET.getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }

    private static void writeJson(HttpExchange exchange, int status, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
