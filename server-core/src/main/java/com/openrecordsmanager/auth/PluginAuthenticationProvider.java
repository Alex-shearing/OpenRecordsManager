package com.openrecordsmanager.auth;

import com.openrecordsmanager.api.auth.PendingRedirectAuth;
import com.openrecordsmanager.api.auth.UserAuthDetails;
import com.openrecordsmanager.auth.entity.AuthProvider;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.user.User;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public class PluginAuthenticationProvider implements AuthenticationProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginAuthenticationProvider.class);
    private final DataRepository repository;
    private final ComponentCatalog catalog;
    private final AuthService authService;

    public PluginAuthenticationProvider(DataRepository repository, ComponentCatalog catalog, AuthService authService) {
        this.repository = repository;
        this.catalog = catalog;
        this.authService = authService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!(authentication instanceof AbstractPluginToken token)) {
            throw new AuthenticationServiceException("Incorrect authentication type");
        }

        if (token.getCredentials() == null) {
            throw new AuthenticationCredentialsNotFoundException("Credentials not found in login token");
        }

        AuthProvider provider = this.repository.authProviderRepo.findByIdAndEnabledTrue(token.provider)
                .orElseThrow(() -> new ProviderNotFoundException("Provider " + token.provider + " not found"));

        UserAuthDetails authDetails = provider.login(this.catalog, this.authService, token);

        if (authDetails == null) {
            throw new BadCredentialsException("Username or password is incorrect'");
        }

        User user = this.repository.userRepo.findByUsername(authDetails.getName())
                .orElseThrow(() -> {
                    LOGGER.warn(
                            "No ORM user found for authenticated principal '{}' from provider {}",
                            authDetails.getName(),
                            provider.getId()
                    );
                    return UsernameNotFoundException.fromUsername(authDetails.getName());
                });

        if (!user.isEnabled()) {
            throw new DisabledException("User account is disabled");
        }

        // Ensure the authentication provider used is the same one the user signed up with
        if (user.getAuthProvider() != provider) {
            LOGGER.warn(
                    "User '{}' is linked to provider {} but authenticated with {}",
                    authDetails.getName(),
                    user.getAuthProvider() != null ? user.getAuthProvider().getId() : null,
                    provider.getId()
            );
            throw new BadCredentialsException("The authentication provider used is not valid for this user");
        }

        // Auth success
        token.setAuthenticated(true);
        token.setDetails(user);
        token.authDetails = authDetails;

        return token;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return AbstractPluginToken.class.isAssignableFrom(authentication);
    }

    public static abstract class AbstractPluginToken extends AbstractAuthenticationToken {
        private final UUID provider;

        private @Nullable UserAuthDetails authDetails = null;

        public AbstractPluginToken(UUID provider) {
            super((Collection<? extends GrantedAuthority>) null);
            this.provider = provider;
            this.setAuthenticated(false);
        }

        @Override
        public @Nullable UserAuthDetails getPrincipal() {
            return this.authDetails;
        }

        @Override
        public @Nullable User getDetails() {
            return (User) super.getDetails();
        }
    }

    public static class RedirectToken extends AbstractPluginToken {
        private final URI uri;
        private final PendingRedirectAuth pending;

        public RedirectToken(UUID provider, URI uri, PendingRedirectAuth pending) {
            super(provider);
            this.uri = uri;
            this.pending = pending;
            this.setAuthenticated(false);
        }

        @Override
        public URI getCredentials() {
            return this.uri;
        }

        public PendingRedirectAuth getPending() {
            return this.pending;
        }
    }

    public static class InputToken extends AbstractPluginToken {
        private final Map<String, String> data;

        public InputToken(UUID provider, Map<String, String> data) {
            super(provider);
            this.data = data;
            this.setAuthenticated(false);
        }

        @Override
        public Map<String, String> getCredentials() {
            return this.data;
        }
    }

}
