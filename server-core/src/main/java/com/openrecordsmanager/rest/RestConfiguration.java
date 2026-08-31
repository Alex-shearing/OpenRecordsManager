package com.openrecordsmanager.rest;

import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.audit.AuditContextFilter;
import com.openrecordsmanager.auth.AuthService;
import com.openrecordsmanager.auth.DatabaseTokenAuthenticationFilter;
import com.openrecordsmanager.auth.PluginAuthenticationProvider;
import com.openrecordsmanager.config.ConfigService;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.database.SchemaUpgradeGateFilter;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.function.Supplier;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class RestConfiguration {

    private static final String[] PUBLIC_API_PATHS = {
            "/api/auth/**",
            "/api/web/**",
            "/api/database/**",
            "/api/health",
            "/api/health/**"
    };

    private static final HttpMethod[] CORS_METHODS = {
            HttpMethod.GET,
            HttpMethod.POST,
            HttpMethod.PUT,
            HttpMethod.PATCH,
            HttpMethod.DELETE,
            HttpMethod.OPTIONS
    };

    private final DataRepository repository;
    private final AuthService authService;
    private final List<String> allowedOrigins;
    private final List<String> allowedHeaders;
    private final boolean cookieSecure;

    public RestConfiguration(
            DataRepository repository,
            AuthService authService,
            ConfigService configService
    ) {
        this.repository = repository;
        this.authService = authService;
        this.allowedOrigins = configService.getOrThrow(BuiltinConfigs.CORS_ALLOWED_ORIGINS);
        this.allowedHeaders = configService.getOrThrow(BuiltinConfigs.CORS_ALLOWED_HEADERS);
        this.cookieSecure = configService.getOrThrow(BuiltinConfigs.COOKIE_SECURE);
    }

    @Bean
    PluginAuthenticationProvider authenticationProvider(
            ComponentCatalog catalog,
            ConfigService config
    ) {
        return new PluginAuthenticationProvider(this.repository, catalog, config, this.authService);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        for (HttpMethod method : CORS_METHODS) {
            configuration.addAllowedMethod(method);
        }

        configuration.setAllowedOrigins(this.allowedOrigins);
        configuration.setAllowedHeaders(this.allowedHeaders);
        configuration.setAllowCredentials(true);
        // Required for browser preflights from localhost / private-network contexts
        configuration.setAllowPrivateNetwork(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            DatabaseTokenAuthenticationFilter tokenAuthenticationFilter,
            SchemaUpgradeGateFilter schemaUpgradeFilter,
            AuditContextFilter auditContextFilter
    ) {
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokenRepository.setCookieCustomizer(cookie -> {
            cookie.secure(this.cookieSecure);
            cookie.sameSite(this.cookieSecure ? "None" : "Lax");
            cookie.path("/");
        });

        http
                .securityMatcher("/api/**")
                .cors(cors -> cors.configurationSource(this.corsConfigurationSource()))
                .csrf(csrf -> csrf
                        // Only enable CSRF protection when using cookie authentication, not when using the header
                        .ignoringRequestMatchers(request -> {
                            String authHeader = request.getHeader("Authorization");
                            return authHeader != null && authHeader.startsWith("Bearer ");
                        })
                        .ignoringRequestMatchers(PUBLIC_API_PATHS)
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler() {
                            @Override
                            public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
                                csrfToken.get();
                                super.handle(request, response, csrfToken);
                            }
                        })
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_API_PATHS).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(auditContextFilter, DatabaseTokenAuthenticationFilter.class)
                .addFilterBefore(schemaUpgradeFilter, DatabaseTokenAuthenticationFilter.class)
                .exceptionHandling(exception -> exception
                        // Force 401 Unauthorized for unauthenticated requests
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                );

        return http.build();
    }
}
