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
import com.openrecordsmanager.auth.dto.*;
import com.openrecordsmanager.auth.entity.AuthProvider;
import com.openrecordsmanager.config.ConfigService;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.database.DatabaseWritableProbe;
import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.plugin.registry.mapper.TemplateRegistrationMapper;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.rest.errors.ResourceNotFoundException;
import com.openrecordsmanager.user.User;
import io.jsonwebtoken.Claims;
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
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuthService implements UserAuthContext {

    private static final String CLIENT_PLATFORM_HEADER_NAME = "X-Client-Platform";

    private final DataRepository repository;
    private final ComponentCatalog catalog;
    private final ExpressionsService expressions;
    private final AuditService auditService;
    private final PluginAuthenticationProvider authenticationProvider;
    private final JwtSessionService jwtSessionService;
    private final DatabaseWritableProbe probe;
    private final String cookieName;
    private final String refreshCookieName;
    private final boolean cookieSecure;

    public AuthService(
            DataRepository repository,
            ComponentCatalog catalog,
            ConfigService config,
            ExpressionsService expressions,
            AuditService auditService,
            @Lazy PluginAuthenticationProvider authenticationProvider,
            JwtSessionService jwtSessionService,
            DatabaseWritableProbe probe
    ) {
        this.repository = repository;
        this.catalog = catalog;
        this.expressions = expressions;
        this.auditService = auditService;
        this.authenticationProvider = authenticationProvider;
        this.jwtSessionService = jwtSessionService;
        this.probe = probe;
        this.cookieName = config.getOrThrow(BuiltinConfigs.COOKIE_NAME);
        this.refreshCookieName = config.getOrThrow(BuiltinConfigs.REFRESH_COOKIE_NAME);
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

    @Transactional(readOnly = true)
    public LoginResponse login(
            PluginAuthenticationProvider.AbstractPluginToken token,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws AuthenticationException {
        Authentication authenticatedUser = this.authenticationProvider.authenticate(token);
        if (!authenticatedUser.isAuthenticated() || authenticatedUser.getDetails() == null) {
            throw new BadCredentialsException("Username or password is incorrect");
        }

        User user = (User) authenticatedUser.getDetails();
        TokenPair pair = this.issueTokenPair(user);
        this.applyCookiesIfWebClient(request, response, pair);
        return LoginResponse.of(pair);
    }

    @Transactional(readOnly = true)
    public LoginResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = this.extractTokenFromRequest(request, this.refreshCookieName);
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BadCredentialsException("Refresh token is required");
        }

        Claims claims = this.jwtSessionService.verifyRefreshToken(refreshToken);
        User user = this.jwtSessionService.resolveUser(claims);
        TokenPair pair = this.issueTokenPair(user);
        this.applyCookiesIfWebClient(request, response, pair);
        return LoginResponse.of(pair);
    }

    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = this.extractTokenFromRequest(request, this.cookieName);
        if (accessToken != null && this.probe.isWritable()) {
            try {
                Claims claims = this.jwtSessionService.verifyAccessToken(accessToken);
                UUID userId = UUID.fromString(claims.getSubject());
                this.repository.userRepo.findById(userId).ifPresent(user -> {
                    user.bumpSessionEpoch();
                    this.repository.userRepo.saveAndFlush(user);
                });
            } catch (Exception ignored) {
                // Cookie/token may already be invalid; still clear cookies below.
            }
        }

        this.clearAuthCookies(response);
        SecurityContextHolder.clearContext();
    }

    public TokenPair issueTokenPair(User user) {
        SessionMode mode = this.probe.isWritable() ? SessionMode.NORMAL : SessionMode.DEGRADED_READ_ONLY;
        return this.jwtSessionService.issueTokenPair(user, mode);
    }

    @Transactional
    public void bumpSessionEpochForAuthProvider(UUID authProviderId) {
        this.repository.userRepo.findByAuthProvider_Id(authProviderId).forEach(user -> {
            user.bumpSessionEpoch();
            this.repository.userRepo.save(user);
        });
        this.repository.userRepo.flush();
    }

    public @Nullable String extractTokenFromRequest(HttpServletRequest request, String cookieName) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            if (authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
            return null;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(cookie -> cookie.getName().equals(cookieName))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);

    }

    private void applyCookiesIfWebClient(HttpServletRequest request, HttpServletResponse response, TokenPair pair) {
        if (!"Web-Client".equals(request.getHeader(CLIENT_PLATFORM_HEADER_NAME))) {
            return;
        }
        this.setCookie(response, this.cookieName, pair.accessToken(), secondsUntil(pair.accessExpires()));
        this.setCookie(response, this.refreshCookieName, pair.refreshToken(), secondsUntil(pair.refreshExpires()));
    }

    private void clearAuthCookies(HttpServletResponse response) {
        this.setCookie(response, this.cookieName, "", 0);
        this.setCookie(response, this.refreshCookieName, "", 0);
    }

    private void setCookie(HttpServletResponse response, String name, String value, long durationSeconds) {
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge((int) Math.min(durationSeconds, Integer.MAX_VALUE));
        cookie.setHttpOnly(true);
        cookie.setSecure(this.cookieSecure);
        cookie.setPath("/");
        cookie.setAttribute("SameSite", this.cookieSecure ? "None" : "Lax");
        response.addCookie(cookie);
    }

    private static long secondsUntil(Instant expires) {
        return Math.max(0, expires.getEpochSecond() - Instant.now().getEpochSecond());
    }

    public String getCookieName() {
        return this.cookieName;
    }

    public String getRefreshCookieName() {
        return this.refreshCookieName;
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
            changes.add(AuditPropertyChange.of("name", oldName, input.name()));
        }

        if (input.settings() != null) {
            Map<String, Object> oldSettings = new HashMap<>(provider.settings);
            provider.settings = new HashMap<>(input.settings());
            changes.add(AuditPropertyChange.of("settings", oldSettings.keySet(), input.settings().keySet()));
        }

        if (input.enabled() != null && input.enabled() != provider.isEnabled()) {
            boolean oldEnabled = provider.isEnabled();
            provider.setEnabled(input.enabled());
            changes.add(AuditPropertyChange.of("enabled", oldEnabled, input.enabled()));

            if (!input.enabled()) {
                this.bumpSessionEpochForAuthProvider(provider.getId());
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
