package com.openrecordsmanager.auth;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.auth.AuthProviderType;
import com.openrecordsmanager.api.auth.RedirectAuthProviderType;
import com.openrecordsmanager.api.auth.UserAuthContext;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.template.property.ObjectPropertyTemplate;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.audit.RequiresAuditComment;
import com.openrecordsmanager.auth.dto.AuthProviderListResponse;
import com.openrecordsmanager.auth.dto.LoginResponse;
import com.openrecordsmanager.auth.entity.AuthProvider;
import com.openrecordsmanager.auth.entity.AuthToken;
import com.openrecordsmanager.config.ConfigService;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.plugin.registry.mapper.TemplateRegistrationMapper;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.rest.errors.ResourceNotFoundException;
import com.openrecordsmanager.user.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuthService implements UserAuthContext {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final DataRepository repository;
    private final ComponentCatalog catalog;
    private final ExpressionsService expressions;
    private final AuditService auditService;
    private final PluginAuthenticationProvider authenticationProvider;
    private final String cookieName;
    private final long tokenDuration;
    private final boolean cookieSecure;

    public AuthService(
            DataRepository repository,
            ComponentCatalog catalog,
            ConfigService config,
            ExpressionsService expressions,
            AuditService auditService,
            @Lazy PluginAuthenticationProvider authenticationProvider
    ) {
        this.repository = repository;
        this.catalog = catalog;
        this.expressions = expressions;
        this.auditService = auditService;
        this.authenticationProvider = authenticationProvider;
        // Cache these configuration options at startup, don't query sources each time its used
        this.cookieName = config.getOrThrow(BuiltinConfigs.COOKIE_NAME);
        this.tokenDuration = config.getOrThrow(BuiltinConfigs.TOKEN_EXPIRATION_TIME);
        this.cookieSecure = config.getOrThrow(BuiltinConfigs.COOKIE_SECURE);
    }

    public Set<AuthProviderListResponse> listProviders() {
        return this.repository.authProviderRepo.findAll().stream()
                .map(provider -> AuthProviderListResponse.of(this.catalog, provider))
                .collect(Collectors.toSet());
    }

    public URI getRedirectLocation(UUID authProviderId) {
        AuthProvider provider = this.repository.authProviderRepo.findById(authProviderId)
                .orElseThrow(() -> new ResourceNotFoundException("authentication provider", authProviderId.toString()));
        RedirectAuthProviderType type = provider.getProviderType(this.catalog, RedirectAuthProviderType.class);
        return type.getRedirectTo(provider);
    }

    public LoginResponse login(
            PluginAuthenticationProvider.AbstractPluginToken token,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws AuthenticationException {
        Authentication authenticatedUser = this.authenticationProvider.authenticate(token);
        if (!authenticatedUser.isAuthenticated() || authenticatedUser.getDetails() == null) {
            throw new BadCredentialsException("Username or password is incorrect");
        }

        AuthToken persistedToken = this.generateToken((User) authenticatedUser.getDetails());

        // Add the token as a cookie if the request has come from the web client
        if ("Web-Client".equals(request.getHeader("X-Client-Platform"))) {
            Cookie cookie = new Cookie(this.getCookieName(), persistedToken.getToken());
            cookie.setMaxAge((int) this.tokenDuration);
            cookie.setHttpOnly(true);
            cookie.setSecure(this.cookieSecure);
            cookie.setPath("/");
            // SameSite=None requires Secure; use Lax for local HTTP deployments
            cookie.setAttribute("SameSite", this.cookieSecure ? "None" : "Lax");
            response.addCookie(cookie);
        }

        return LoginResponse.of(persistedToken);
    }

    public static String generateToken() {
        byte[] randomBytes = new byte[32]; // 256 bits of entropy
        RANDOM.nextBytes(randomBytes);
        String token = ENCODER.encodeToString(randomBytes);

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private AuthToken generateToken(User details) {
        AuthToken token = new AuthToken(generateToken(), details, Instant.now().plusSeconds(this.tokenDuration));
        return this.repository.authTokenRepo.save(token);
    }

    public String getCookieName() {
        return cookieName;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getUserProperty(String username, ObjectPropertyTemplate<T> property) {
        Optional<ObjectProperty<?>> prop = this.catalog.getTemplateRegistry(ComponentCatalog.OBJECT_PROPERTY_MAPPER)
                .getRegistered(property, this.repository);

        if (prop.isEmpty()) {
            return Optional.empty();
        }

        ObjectProperty<T> typedProp = (ObjectProperty<T>) prop.get();

        return this.repository.userRepo.findByUsername(username)
                .map(user -> user.getProperty(typedProp));
    }

    @RequiresAuditComment(operation = AuditOperation.CREATE, targetType = AuditEntityType.AUTH_PROVIDER)
    public AuthProviderListResponse createProvider(String name, ComponentReference<? extends AuthProviderType> type, Map<String, Object> settings) {
        // Ensure any pre-requisite templates are registered
        TemplateRegistrationMapper.registerDependencies(
                this.repository,
                this.catalog,
                this.expressions,
                this.auditService,
                type.getComponent(this.catalog)
                        .orElseThrow(() -> new IllegalArgumentException("unknown auth provider type " + type))
        );

        AuthProvider provider = new AuthProvider(name, type, settings);

        this.repository.authProviderRepo.save(provider);

        this.auditService.addEvent(AuditOperation.CREATE, AuditEntityType.AUTH_PROVIDER, provider.getId());

        return AuthProviderListResponse.of(this.catalog, provider);
    }
}
