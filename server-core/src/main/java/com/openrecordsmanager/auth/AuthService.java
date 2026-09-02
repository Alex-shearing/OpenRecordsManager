package com.openrecordsmanager.auth;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.auth.AuthProviderType;
import com.openrecordsmanager.api.auth.RedirectAuthProviderType;
import com.openrecordsmanager.api.auth.UserAuthContext;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.template.property.ObjectPropertyTemplate;
import com.openrecordsmanager.audit.AuditPropertyChange;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.audit.RequiresAuditComment;
import com.openrecordsmanager.auth.dto.AuthProviderResponse;
import com.openrecordsmanager.auth.dto.LoginResponse;
import com.openrecordsmanager.auth.dto.UpdateAuthProviderRequest;
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
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    public Set<AuthProviderResponse> listProviders(boolean includeDisabled) {
        return this.repository.authProviderRepo.findAll().stream()
                .filter(p -> includeDisabled || p.isEnabled())
                .map(provider -> AuthProviderResponse.of(this.catalog, provider))
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public URI getRedirectLocation(UUID authProviderId) {
        AuthProvider provider = this.repository.authProviderRepo.findById(authProviderId)
                .orElseThrow(() -> new ResourceNotFoundException("authentication provider", authProviderId.toString()));
        if (!provider.isEnabled()) {
            throw new ResourceNotFoundException("authentication provider", authProviderId.toString());
        }
        RedirectAuthProviderType type = provider.getProviderType(this.catalog, RedirectAuthProviderType.class);
        return type.getRedirectTo(provider);
    }

    @Transactional
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
            this.setAuthCookie(response, persistedToken.getToken(), this.tokenDuration);
        }

        return LoginResponse.of(persistedToken);
    }

    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String tokenValue = this.extractTokenFromRequest(request);
        if (tokenValue != null) {
            this.repository.authTokenRepo.deleteById(tokenValue);
        }

        this.setAuthCookie(response, "", 0);
        SecurityContextHolder.clearContext();
    }

    public @Nullable String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            Optional<Cookie> authCookie = Arrays.stream(cookies)
                    .filter(cookie -> cookie.getName().equals(this.cookieName))
                    .findFirst();

            if (authCookie.isPresent() && !authCookie.get().getValue().isBlank()) {
                return authCookie.get().getValue();
            }
        }

        return null;
    }

    private void setAuthCookie(HttpServletResponse response, String value, long duration) {
        Cookie cookie = new Cookie(this.cookieName, value);
        cookie.setMaxAge((int) duration);
        cookie.setHttpOnly(true);
        cookie.setSecure(this.cookieSecure);
        cookie.setPath("/");
        cookie.setAttribute("SameSite", this.cookieSecure ? "None" : "Lax");
        response.addCookie(cookie);
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
        return this.cookieName;
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
    public AuthProviderResponse createProvider(String name, ComponentReference<? extends AuthProviderType> type, Map<String, Object> settings) {
        // Ensure any pre-requisite templates are registered
        TemplateRegistrationMapper.registerDependencies(
                this.repository,
                this.catalog,
                this.expressions,
                this.auditService,
                type.getComponent(this.catalog)
                        .orElseThrow(() -> new ResourceNotFoundException("authentication provider", type.toString()))
        );

        AuthProvider provider = new AuthProvider(name, type, settings);

        this.repository.authProviderRepo.save(provider);

        this.auditService.addEvent(AuditOperation.CREATE, AuditEntityType.AUTH_PROVIDER, provider.getId());

        return AuthProviderResponse.of(this.catalog, provider);
    }

    @Transactional
    @RequiresAuditComment(operation = AuditOperation.UPDATE, targetType = AuditEntityType.AUTH_PROVIDER)
    public AuthProviderResponse updateProvider(UUID id, UpdateAuthProviderRequest input) {
        AuthProvider provider = this.repository.authProviderRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("authentication provider", id));

        List<AuditPropertyChange> changes = new ArrayList<>();

        if (input.name() != null && !input.name().equals(provider.name)) {
            String oldName = provider.name;
            provider.name = input.name();
            changes.add(new AuditPropertyChange("name", oldName, input.name()));
        }

        if (input.settings() != null) {
            Map<String, Object> oldSettings = new HashMap<>(provider.settings);
            provider.settings = new HashMap<>(input.settings());
            changes.add(new AuditPropertyChange("settings", oldSettings.keySet(), input.settings().keySet()));
        }

        if (input.enabled() != null && input.enabled() != provider.isEnabled()) {
            boolean oldEnabled = provider.isEnabled();
            provider.setEnabled(input.enabled());
            changes.add(new AuditPropertyChange("enabled", oldEnabled, input.enabled()));

            if (!input.enabled()) {
                this.repository.authTokenRepo.deleteByUser_AuthProvider_Id(provider.getId());
            }
        }

        this.repository.authProviderRepo.saveAndFlush(provider);

        this.auditService.addEvent(
                AuditOperation.UPDATE,
                AuditEntityType.AUTH_PROVIDER,
                provider.getId().toString(),
                changes.isEmpty() ? null : changes,
                null,
                null
        );

        return AuthProviderResponse.of(this.catalog, provider);
    }
}
