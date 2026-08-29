package com.openrecordsmanager.auth;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.auth.AuthProviderType;
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
import com.openrecordsmanager.user.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService implements UserAuthContext {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final DataRepository repository;
    private final ComponentCatalog catalog;
    private final ConfigService config;
    private final ExpressionsService expressions;
    private final AuditService auditService;
    private final String cookieName;
    private final long tokenDuration;
    private final boolean cookieSecure;

    public AuthService(
            DataRepository repository,
            ComponentCatalog catalog,
            ConfigService config,
            ExpressionsService expressions,
            AuditService auditService
    ) {
        this.repository = repository;
        this.catalog = catalog;
        this.config = config;
        this.expressions = expressions;
        this.auditService = auditService;
        // Cache these configuration options at startup, don't query sources each time its used
        this.cookieName = config.getOrThrow(BuiltinConfigs.COOKIE_NAME);
        this.tokenDuration = config.getOrThrow(BuiltinConfigs.TOKEN_EXPIRATION_TIME);
        this.cookieSecure = config.getOrThrow(BuiltinConfigs.COOKIE_SECURE);
    }

    @Bean
    UserDetailsService userDetailsService() {
        return username -> this.repository.userRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Bean
    AuthenticationProvider authenticationProvider() {
        return new PluginAuthenticationProvider(this.repository, this.catalog, this.config, this);
    }

    public LoginResponse login(
            PluginAuthenticationProvider.AbstractPluginToken token,
            HttpServletRequest request,
            HttpServletResponse response) throws AuthenticationException {
        Authentication authenticatedUser = this.authenticationProvider().authenticate(token);
        if (authenticatedUser == null || !authenticatedUser.isAuthenticated() || authenticatedUser.getDetails() == null) {
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
                this.repository, this.catalog, this.expressions,
                type.getComponent(this.catalog)
                        .orElseThrow(() -> new IllegalArgumentException("unknown auth provider type " + type))
        );

        AuthProvider provider = new AuthProvider(name, type, settings);

        this.repository.authProviderRepo.save(provider);

        this.auditService.addEvent(AuditOperation.CREATE, AuditEntityType.AUTH_PROVIDER, provider.getId());

        return AuthProviderListResponse.of(this.catalog, provider);
    }
}
