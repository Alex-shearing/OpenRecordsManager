package com.openrecordsmanager.auth;

import com.openrecordsmanager.database.DataRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
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
public class SecurityConfiguration {
    private final DataRepository repository;
    private final AuthService authService;
    private final List<String> allowedOrigins;
    private final List<String> allowedMethods;
    private final List<String> allowedHeaders;
    private final boolean cookieSecure;

    public SecurityConfiguration(
            DataRepository repository,
            AuthService authService,
            @Value("${app.security.cors.allowed-origins:http://localhost:5173,http://localhost:3000}") List<String> allowedOrigins,
            @Value("${app.security.cors.allowed-methods:GET,POST,PUT,PATCH,DELETE,OPTIONS}") List<String> allowedMethods,
            @Value("${app.security.cors.allowed-headers:Authorization,Content-Type,X-XSRF-TOKEN,X-Client-Platform}") List<String> allowedHeaders,
            @Value("${app.security.cookie-secure:true}") boolean cookieSecure
    ) {
        this.repository = repository;
        this.authService = authService;
        this.allowedOrigins = allowedOrigins;
        this.allowedMethods = allowedMethods;
        this.allowedHeaders = allowedHeaders;
        this.cookieSecure = cookieSecure;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(this.allowedOrigins);
        configuration.setAllowedMethods(this.allowedMethods);
        configuration.setAllowedHeaders(this.allowedHeaders);
        configuration.setAllowCredentials(true);
        // Required for browser preflights from localhost / private-network contexts
        configuration.setAllowPrivateNetwork(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
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
                        .ignoringRequestMatchers("/api/auth/**", "/v3/api-docs/**")
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
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new DatabaseTokenAuthenticationFilter(this.repository.authTokenRepo, this.authService), UsernamePasswordAuthenticationFilter.class)
                .authenticationProvider(this.authService.authenticationProvider())
                .exceptionHandling(exception -> exception
                        // Force 401 Unauthorized for unauthenticated requests
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                );

        return http.build();
    }
}
