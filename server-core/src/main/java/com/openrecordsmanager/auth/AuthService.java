package com.openrecordsmanager.auth;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.auth.AuthProviderType;
import com.openrecordsmanager.api.auth.PendingRedirectAuth;
import com.openrecordsmanager.api.auth.RedirectAuthChallenge;
import com.openrecordsmanager.api.auth.UserAuthContext;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.template.property.ObjectPropertyTemplate;
import com.openrecordsmanager.api.types.ComponentTypes;
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
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
import java.util.stream.Stream;

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
    private final PendingRedirectAuthCookie pendingRedirectAuthCookie;
    private final CookieService cookies;
    private final String cookieName;
    private final String refreshCookieName;
    private final String publicBaseUrl;

    public AuthService(
            DataRepository repository,
            ComponentCatalog catalog,
            ConfigService config,
            ExpressionsService expressions,
            AuditService auditService,
            @Lazy PluginAuthenticationProvider authenticationProvider,
            JwtSessionService jwtSessionService,
            DatabaseWritableProbe probe,
            PendingRedirectAuthCookie pendingRedirectAuthCookie,
            CookieService cookies
    ) {
        this.repository = repository;
        this.catalog = catalog;
        this.expressions = expressions;
        this.auditService = auditService;
        this.authenticationProvider = authenticationProvider;
        this.jwtSessionService = jwtSessionService;
        this.probe = probe;
        this.pendingRedirectAuthCookie = pendingRedirectAuthCookie;
        this.cookies = cookies;
        this.cookieName = config.getOrThrow(BuiltinConfigs.COOKIE_NAME);
        this.refreshCookieName = config.getOrThrow(BuiltinConfigs.REFRESH_COOKIE_NAME);
        this.publicBaseUrl = trimTrailingSlash(config.getOrThrow(BuiltinConfigs.PUBLIC_BASE_URL));
    }

    @Transactional(readOnly = true)
    public Set<SimpleAuthProviderResponse> listProviders() {
        return this.repository.authProviderRepo.findByEnabledTrue().stream()
                .map(provider -> SimpleAuthProviderResponse.of(this.catalog, provider))
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public Set<AuthProviderResponse> listAllProviders() {
        return this.repository.authProviderRepo.findAll().stream()
                .map(provider -> AuthProviderResponse.of(this.catalog, provider))
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public AuthProviderResponse getProvider(UUID id) {
        return this.repository.authProviderRepo.findById(id)
                .map(provider -> AuthProviderResponse.of(this.catalog, provider))
                .orElseThrow(() -> new ResourceNotFoundException("authentication provider", id));
    }

    /**
     * Starts a redirect-based login: persists pending OAuth state in a signed cookie and returns
     * the IdP authorization URI.
     */
    @Transactional(readOnly = true)
    public URI beginRedirectLogin(
            UUID authProviderId,
            @Nullable String returnTo,
            HttpServletResponse response
    ) {
        AuthProvider provider = this.repository.authProviderRepo.findByIdAndEnabledTrue(authProviderId)
                .orElseThrow(() -> new ResourceNotFoundException("authentication provider", authProviderId.toString()));

        RedirectAuthChallenge challenge = provider.beginRedirectLogin(this.catalog, this.publicBaseUrl);

        PendingRedirectAuth pending = new PendingRedirectAuth(
                authProviderId,
                challenge.state(),
                challenge.attributes()
        );
        this.pendingRedirectAuthCookie.store(
                response,
                pending,
                SafeRelativePaths.sanitizeOrNull(returnTo)
        );
        return challenge.redirectUri();
    }

    /**
     * Completes a redirect-based browser login: validates pending state, issues session cookies,
     * and returns the safe relative path to send the browser to.
     */
    @Transactional(readOnly = true)
    public URI completeRedirectLogin(
            UUID authProviderId,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        PendingRedirectAuthCookie.Loaded loaded = this.pendingRedirectAuthCookie.take(
                request,
                response,
                authProviderId
        );
        if (loaded == null) {
            throw new BadCredentialsException("Missing or invalid redirect authentication state");
        }

        URI fullCallbackUri = this.buildFullCallbackUri(authProviderId, request.getQueryString());

        Authentication authenticatedUser = this.authenticationProvider.authenticate(
                new PluginAuthenticationProvider.RedirectToken(
                        authProviderId,
                        fullCallbackUri,
                        loaded.pending()
                )
        );
        if (!authenticatedUser.isAuthenticated() || authenticatedUser.getDetails() == null) {
            throw new BadCredentialsException("Username or password is incorrect");
        }

        User user = (User) authenticatedUser.getDetails();
        TokenPair pair = this.issueTokenPair(user);
        this.setAuthCookies(response, pair);
        return URI.create(SafeRelativePaths.orElse(loaded.returnTo(), "/"));
    }

    private URI buildFullCallbackUri(UUID authProviderId, @Nullable String queryString) {
        String base = URI.create(this.publicBaseUrl + "/api/auth/callback/" + authProviderId).toString();
        if (StringUtils.isBlank(queryString)) {
            return URI.create(base);
        }
        return URI.create(base + "?" + queryString);
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
        return this.cookies.read(request, cookieName);
    }

    private void applyCookiesIfWebClient(HttpServletRequest request, HttpServletResponse response, TokenPair pair) {
        if (!"Web-Client".equals(request.getHeader(CLIENT_PLATFORM_HEADER_NAME))) {
            return;
        }
        this.setAuthCookies(response, pair);
    }

    private void setAuthCookies(HttpServletResponse response, TokenPair pair) {
        this.cookies.write(response, this.cookieName, pair.accessToken(), secondsUntil(pair.accessExpires()), "/");
        this.cookies.write(response, this.refreshCookieName, pair.refreshToken(), secondsUntil(pair.refreshExpires()), "/");
    }

    public void clearAuthCookies(HttpServletResponse response) {
        this.cookies.clear(response, this.cookieName, "/");
        this.cookies.clear(response, this.refreshCookieName, "/");
        this.pendingRedirectAuthCookie.clear(response);
    }

    private static long secondsUntil(Instant expires) {
        return Math.max(0, expires.getEpochSecond() - Instant.now().getEpochSecond());
    }

    private static String trimTrailingSlash(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    public String getCookieName() {
        return this.cookieName;
    }

    public String getRefreshCookieName() {
        return this.refreshCookieName;
    }

    @Override
    public <T> Optional<T> getUserProperty(String username, ObjectPropertyTemplate<T> property) {
        Optional<ObjectProperty<?>> prop = this.catalog.getTemplateRegistry(ComponentCatalog.OBJECT_PROPERTY_MAPPER)
                .getRegistered(property, this.repository);

        if (prop.isEmpty()) {
            return Optional.empty();
        }

        @SuppressWarnings("unchecked")
        ObjectProperty<T> typedProp = (ObjectProperty<T>) prop.get();

        return this.repository.userRepo.findByUsername(username)
                .map(user -> user.getProperty(typedProp));
    }

    @RequiresAuditComment(operation = AuditOperation.CREATE, targetType = AuditEntityType.AUTH_PROVIDER)
    public AuthProviderResponse createProvider(
            String name,
            ComponentReference<? extends AuthProviderType<?>> type,
            Map<String, ?> settings
    ) {
        AuthProviderType<?> providerType = type.getComponent(this.catalog)
                .orElseThrow(() -> new ResourceNotFoundException("authentication provider", type.toString()));

        TemplateRegistrationMapper.registerDependencies(
                this.repository,
                this.catalog,
                this.expressions,
                this.auditService,
                providerType
        );

        AuthProvider provider = new AuthProvider(this.catalog, name, type, settings);
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

        if (input.name() != null && !input.name().equals(provider.getName())) {
            String oldName = provider.getName();
            provider.setName(input.name());
            changes.add(AuditPropertyChange.of("name", oldName, input.name()));
        }

        if (input.settings() != null) {
            Map<String, ?> oldSettings = new HashMap<>(provider.getSettings(this.catalog));
            provider.setProperties(this.catalog, input.settings());
            changes.add(AuditPropertyChange.of("settings", oldSettings.keySet(), provider.getSettings(this.catalog).keySet()));
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

    @Transactional(readOnly = true)
    public AuthProviderTypeResponse[] listProviderTypes() {
        Stream<AuthProviderType<?>> input = this.catalog.getRegistry(ComponentTypes.INPUT_AUTH_PROVIDER).stream()
                .map(i -> i);
        Stream<AuthProviderType<?>> redirect = this.catalog.getRegistry(ComponentTypes.REDIRECT_AUTH_PROVIDER).stream()
                .map(i -> (AuthProviderType<?>) i);
        return Stream.concat(input, redirect)
                .map(type -> AuthProviderTypeResponse.of(this.catalog, type))
                .toArray(AuthProviderTypeResponse[]::new);
    }
}
