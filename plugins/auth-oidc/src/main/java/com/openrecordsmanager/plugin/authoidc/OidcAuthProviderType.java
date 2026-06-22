package com.openrecordsmanager.plugin.authoidc;

import com.nimbusds.jwt.JWT;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.openid.connect.sdk.*;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderConfigurationRequest;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import com.openrecordsmanager.api.auth.AuthProviderInstance;
import com.openrecordsmanager.api.auth.RedirectAuthProviderType;
import com.openrecordsmanager.api.auth.UserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class OidcAuthProviderType extends RedirectAuthProviderType {
    private static final Logger LOGGER = LoggerFactory.getLogger(OidcAuthProviderType.class);

    @Override
    public String id() {
        return "oidc_auth";
    }

    @Override
    public URI getRedirectTo(AuthProviderInstance instance) {
        try {
            // The client callback URL
            URI callback = new URI("https://localhost:8080/api/v1/auth/callback/%s".formatted(instance.getId()));
            OidcSettings settings = OidcSettings.parse(instance.getSettings());

            // Generate random state string to securely pair the callback to this request
//            State state = new State();

            // Generate nonce for the ID token
            Nonce nonce = new Nonce();

            // Compose the OpenID authentication request (for the code flow)
            AuthenticationRequest request = new AuthenticationRequest.Builder(
                    new ResponseType("code"),
                    settings.scope(),
                    settings.clientID(),
                    callback)
                    .endpointURI(settings.metadata().getAuthorizationEndpointURI())
//                    .state(state)
                    .nonce(nonce)
                    .build();

            return request.toURI();
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public UserDetails authenticateCallback(AuthProviderInstance instance, URI uri) {
        try {
            AuthenticationResponse response = AuthenticationResponseParser.parse(uri);
            OidcSettings settings = OidcSettings.parse(instance.getSettings());

            // Check the state
//            if (!response.getState().equals(state)) {
//                System.err.println("Unexpected authentication response");
//                return;
//            }

            if (!response.indicatesSuccess()) {
                LOGGER.error("Authentication error response received: {}", response.toErrorResponse().getErrorObject());
                return null;
            }


            AuthorizationCode code = response.toSuccessResponse().getAuthorizationCode();
            URI callback = new URI("https://localhost:8080/api/v1/auth/callback/%s".formatted(instance.getId()));
            AuthorizationGrant codeGrant = new AuthorizationCodeGrant(code, callback);

            ClientAuthentication clientAuth = new ClientSecretBasic(settings.clientID(), settings.secret());

            // Make the token request
            TokenRequest request = new TokenRequest.Builder(settings.metadata().getTokenEndpointURI(), clientAuth, codeGrant).build();

            TokenResponse tokenResponse = OIDCTokenResponseParser.parse(request.toHTTPRequest().send());

            if (!tokenResponse.indicatesSuccess()) {
                // We got an error response...
                TokenErrorResponse errorResponse = tokenResponse.toErrorResponse();
                LOGGER.error("Authorization error response received: {}", errorResponse.getErrorObject());
            }


            OIDCTokenResponse successResponse = (OIDCTokenResponse) tokenResponse.toSuccessResponse();

            // Get the ID and access token, the server may also return a refresh token
            JWT idToken = successResponse.getOIDCTokens().getIDToken();
            AccessToken accessToken = successResponse.getOIDCTokens().getAccessToken();
            RefreshToken refreshToken = successResponse.getOIDCTokens().getRefreshToken();

            System.out.println(code);

            return new UserDetails(instance, idToken.getJWTClaimsSet().getClaimAsString("name"), "admin");
        } catch (ParseException e) {
            LOGGER.error("OIDC redirect URI parse error: {}", e.getMessage());
            return null;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (java.text.ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private record OidcSettings(ClientID clientID, Secret secret, URI endpointURI, Scope scope,
                                OIDCProviderMetadata metadata) {
        static OidcSettings parse(Map<String, Object> settings) throws URISyntaxException, IOException, ParseException {
            ClientID clientId = new ClientID(settings.get("client_id").toString());
            Secret secret = new Secret(settings.get("secret").toString());
            URI uri = new URI(settings.get("uri").toString());
            Scope scope = new Scope((String[]) settings.get("scope"));

            OIDCProviderConfigurationRequest request = new OIDCProviderConfigurationRequest(new Issuer(uri));

            HTTPRequest httpRequest = request.toHTTPRequest();
            HTTPResponse httpResponse = httpRequest.send();

            OIDCProviderMetadata metadata = OIDCProviderMetadata.parse(httpResponse.getBodyAsJSONObject());

            return new OidcSettings(clientId, secret, uri, scope, metadata);
        }
    }
}
