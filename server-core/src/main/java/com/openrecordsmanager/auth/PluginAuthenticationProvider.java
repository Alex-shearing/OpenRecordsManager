package com.openrecordsmanager.auth;

import com.openrecordsmanager.api.auth.AuthProviderType;
import com.openrecordsmanager.api.auth.InputAuthProviderType;
import com.openrecordsmanager.api.auth.RedirectAuthProviderType;
import com.openrecordsmanager.api.auth.UserAuthDetails;
import com.openrecordsmanager.auth.entity.AuthProvider;
import com.openrecordsmanager.config.ConfigService;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.user.User;
import org.jspecify.annotations.Nullable;
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
    private final DataRepository repository;
    private final ComponentCatalog catalog;
    private final ConfigService config;
    private final AuthService authService;

    public PluginAuthenticationProvider(DataRepository repository, ComponentCatalog catalog, ConfigService config, AuthService authService) {
        this.repository = repository;
        this.catalog = catalog;
        this.config = config;
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

        AuthProvider provider = this.repository.authProviderRepo.findById(token.provider)
                .orElseThrow(() -> new ProviderNotFoundException("Provider " + token.provider + " not found"));

        AuthProviderType type = provider.getProviderType(catalog, AuthProviderType.class);

        UserAuthDetails authDetails = switch (type) {
            case InputAuthProviderType<?> input ->
                    input.authenticateUntyped(this.config, this.authService, provider, ((InputToken) token).data);
            case RedirectAuthProviderType redirect ->
                    redirect.authenticateCallback(provider, this.authService, ((RedirectToken) token).uri);
            default -> throw new InternalAuthenticationServiceException("Unexpected provider type: " + type);
        };

        if (authDetails == null) {
            throw new BadCredentialsException("Username or password is incorrect'");
        }

        User user = this.repository.userRepo.findByUsername(authDetails.getName())
                .orElseThrow(() -> UsernameNotFoundException.fromUsername(authDetails.getName()));

        // Ensure the authentication provider used is the same one the user signed up with
        if (user.authProvider != provider) {
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
        return authentication.equals(InputToken.class);
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

        public RedirectToken(UUID provider, URI uri) {
            super(provider);
            this.uri = uri;
            this.setAuthenticated(false);
        }

        @Override
        public @Nullable URI getCredentials() {
            return this.uri;
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
        public @Nullable Map<String, String> getCredentials() {
            return this.data;
        }
    }

}
