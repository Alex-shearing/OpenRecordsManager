package com.openrecordsmanager.plugin.authoidc;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWT;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import com.nimbusds.openid.connect.sdk.*;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderConfigurationRequest;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;
import com.openrecordsmanager.api.auth.*;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class OidcAuthProviderType extends RedirectAuthProviderType<OidcAuthSettings> {
    private static final Logger LOGGER = LoggerFactory.getLogger(OidcAuthProviderType.class);

    static final String ATTR_NONCE = "nonce";
    static final String ATTR_CODE_VERIFIER = "code_verifier";

    private static final long METADATA_CACHE_TTL_SECONDS = 600;

    private final Cache<String, OIDCProviderMetadata> metadataCache = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofSeconds(METADATA_CACHE_TTL_SECONDS))
            .build();

    protected OidcAuthProviderType() {
        super(OidcAuthSettings.class);
    }

    @Override
    public RedirectAuthChallenge begin(URI callbackUri, OidcAuthSettings cfg) {
        OidcSettings settings = this.resolveSettings(cfg);

        State state = new State();
        Nonce nonce = new Nonce();
        CodeVerifier codeVerifier = new CodeVerifier();

        AuthenticationRequest request = new AuthenticationRequest.Builder(
                new ResponseType(ResponseType.Value.CODE),
                settings.scope(),
                settings.clientID(),
                callbackUri
        )
                .endpointURI(settings.metadata().getAuthorizationEndpointURI())
                .state(state)
                .nonce(nonce)
                .codeChallenge(codeVerifier, CodeChallengeMethod.S256)
                .build();

        return new RedirectAuthChallenge(request.toURI(), state.getValue(), Map.of(
                ATTR_NONCE, nonce.getValue(),
                ATTR_CODE_VERIFIER, codeVerifier.getValue()
        ));
    }

    @Override
    public @Nullable UserAuthDetails complete(
            UserAuthContext context,
            URI fullCallbackUri,
            PendingRedirectAuth pending,
            OidcAuthSettings cfg
    ) {
        OidcSettings settings = null;
        JWT idToken = null;
        try {
            AuthenticationResponse response = AuthenticationResponseParser.parse(fullCallbackUri);
            if (!response.indicatesSuccess()) {
                LOGGER.error("OIDC authentication error: {}", response.toErrorResponse().getErrorObject());
                return null;
            }

            State expectedState = new State(pending.state());
            if (response.getState() == null || !expectedState.equals(response.getState())) {
                LOGGER.warn("OIDC state mismatch");
                return null;
            }

            String nonceValue = pending.attributes().get(ATTR_NONCE);
            String codeVerifierValue = pending.attributes().get(ATTR_CODE_VERIFIER);
            if (nonceValue == null || codeVerifierValue == null) {
                LOGGER.warn("OIDC pending attributes missing nonce or code_verifier");
                return null;
            }

            URI callbackUri = stripQuery(fullCallbackUri);

            AuthorizationCode code = response.toSuccessResponse().getAuthorizationCode();
            AuthorizationCodeGrant codeGrant = new AuthorizationCodeGrant(
                    code,
                    callbackUri,
                    new CodeVerifier(codeVerifierValue)
            );

            settings = this.resolveSettings(cfg);

            ClientAuthentication clientAuth = new ClientSecretBasic(settings.clientID(), settings.secret());
            TokenRequest tokenRequest = new TokenRequest.Builder(
                    settings.metadata().getTokenEndpointURI(),
                    clientAuth,
                    codeGrant
            ).build();

            TokenResponse tokenResponse = OIDCTokenResponseParser.parse(tokenRequest.toHTTPRequest().send());
            if (!tokenResponse.indicatesSuccess()) {
                LOGGER.error("OIDC token error: {}", tokenResponse.toErrorResponse().getErrorObject());
                return null;
            }

            OIDCTokenResponse successResponse = (OIDCTokenResponse) tokenResponse.toSuccessResponse();
            idToken = successResponse.getOIDCTokens().getIDToken();
            if (idToken == null) {
                LOGGER.error("OIDC token response did not include an ID token");
                return null;
            }

            IDTokenClaimsSet claims = this.validateIdToken(settings, idToken, new Nonce(nonceValue));
            String username = resolveUsername(claims, settings.usernameClaim());
            if (username == null || username.isBlank()) {
                LOGGER.error(
                        "OIDC ID token did not contain a usable username (claim '{}', sub={})",
                        settings.usernameClaim(),
                        claims.getSubject()
                );
                return null;
            }

            LOGGER.info(
                    "OIDC login mapped id_token to username '{}' via claim '{}' (sub={})",
                    username,
                    settings.usernameClaim(),
                    claims.getSubject()
            );

            String email = claims.getStringClaim("email");
            if (email == null) {
                email = "";
            }

            return new UserAuthDetails(username, email);
        } catch (ParseException e) {
            LOGGER.error("OIDC callback parse error: {}", e.getMessage());
            return null;
        } catch (BadJOSEException | com.nimbusds.jose.JOSEException e) {
            String tokenIssuer = null;
            try {
                tokenIssuer = idToken.getJWTClaimsSet().getIssuer();
            } catch (Exception ignored) {
                // best-effort diagnostics only
            }
            LOGGER.error(
                    "OIDC ID token validation failed: {} (expected issuer={}, token iss={})",
                    e.getMessage(),
                    settings.metadata().getIssuer(),
                    tokenIssuer
            );
            return null;
        } catch (GeneralException | URISyntaxException | IOException e) {
            LOGGER.error("OIDC completion failed: {}", e.getMessage());
            return null;
        }
    }

    private IDTokenClaimsSet validateIdToken(OidcSettings settings, JWT idToken, Nonce nonce)
            throws BadJOSEException, com.nimbusds.jose.JOSEException, GeneralException, IOException {
        JWSAlgorithm alg = selectIdTokenAlgorithm(settings.metadata());
        IDTokenValidator validator;
        if (JWSAlgorithm.Family.HMAC_SHA.contains(alg)) {
            validator = new IDTokenValidator(
                    settings.metadata().getIssuer(),
                    settings.clientID(),
                    alg,
                    settings.secret()
            );
        } else {
            URI jwksUri = settings.metadata().getJWKSetURI();
            if (jwksUri == null) {
                throw new GeneralException("OIDC provider metadata is missing jwks_uri");
            }
            validator = new IDTokenValidator(
                    settings.metadata().getIssuer(),
                    settings.clientID(),
                    alg,
                    jwksUri.toURL()
            );
        }
        return validator.validate(idToken, nonce);
    }

    static @Nullable String resolveUsername(IDTokenClaimsSet claims, String usernameClaim) {
        String configured = claims.getStringClaim(usernameClaim);
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        return claims.getSubject() != null ? claims.getSubject().getValue() : null;
    }

    private OidcSettings resolveSettings(OidcAuthSettings cfg) {

        return new OidcSettings(
                new ClientID(cfg.clientId()),
                new Secret(cfg.secret()),
                parseScope(cfg.scope()),
                cfg.usernameClaim(),
                this.loadMetadata(URI.create(cfg.uri()))
        );
    }

    private OIDCProviderMetadata loadMetadata(URI issuerUri) {
        try {
            return this.metadataCache.get(issuerUri.toString(), () -> {
                OIDCProviderConfigurationRequest request = new OIDCProviderConfigurationRequest(new Issuer(issuerUri));
                HTTPRequest httpRequest = request.toHTTPRequest();
                URI discoveryUri = httpRequest.getURL().toURI();
                HTTPResponse httpResponse = httpRequest.send();

                int status = httpResponse.getStatusCode();
                if (status / 100 != 2) {
                    throw new IOException(
                            "OIDC discovery failed for issuer %s (GET %s → HTTP %d). Use the OpenID issuer URL (not an IdP admin UI root)."
                                    .formatted(issuerUri, discoveryUri, status)
                    );
                }

                try {
                    return OIDCProviderMetadata.parse(httpResponse.getBodyAsJSONObject());
                } catch (ParseException e) {
                    String contentType = httpResponse.getEntityContentType() != null
                            ? httpResponse.getEntityContentType().toString()
                            : "unknown";
                    throw new IOException(
                            "OIDC discovery for issuer %s (GET %s) returned %s instead of application/json. "
                                    .formatted(issuerUri, discoveryUri, contentType),
                            e
                    );
                }
            });
        } catch (ExecutionException e) {
            LOGGER.error("Failed to cache OIDC metadata for {}", issuerUri, e);
            throw new RuntimeException(e);
        }
    }

    static Scope parseScope(String s) {
        Scope scope = Scope.parse(s);
        if (!scope.contains("openid")) {
            Scope withOpenId = new Scope("openid");
            withOpenId.addAll(scope);
            return withOpenId;
        }
        return scope;
    }

    private static JWSAlgorithm selectIdTokenAlgorithm(OIDCProviderMetadata metadata) {
        List<JWSAlgorithm> algs = metadata.getIDTokenJWSAlgs();
        if (algs != null && !algs.isEmpty()) {
            return algs.getFirst();
        }
        return JWSAlgorithm.RS256;
    }

    private static URI stripQuery(URI uri) throws URISyntaxException {
        return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), null, null);
    }

    private record OidcSettings(
            ClientID clientID,
            Secret secret,
            Scope scope,
            String usernameClaim,
            OIDCProviderMetadata metadata
    ) {
    }
}
