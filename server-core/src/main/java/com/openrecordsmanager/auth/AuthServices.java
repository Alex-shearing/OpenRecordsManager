package com.openrecordsmanager.auth;

import com.openrecordsmanager.auth.dto.LoginResponse;
import com.openrecordsmanager.auth.entity.AuthToken;
import com.openrecordsmanager.config.DynamicConfigService;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.plugin.ComponentCatalog;
import com.openrecordsmanager.user.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
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
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class AuthServices {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final DataRepository repository;
    private final ComponentCatalog catalog;
    private final DynamicConfigService config;
    private final String cookieName;
    private final long tokenDuration;


    public AuthServices(
            DataRepository repository,
            ComponentCatalog catalog,
            DynamicConfigService config,
            @Value("${app.security.cookie-name}") String cookieName,
            @Value("${app.security.expiration-time}") long tokenDuration
    ) {
        this.repository = repository;
        this.catalog = catalog;
        this.config = config;
        this.cookieName = cookieName;
        this.tokenDuration = tokenDuration;
    }

    @Bean
    UserDetailsService userDetailsService() {
        return username -> this.repository.userRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Bean
    AuthenticationProvider authenticationProvider() {
        return new PluginAuthenticationProvider(this.repository, this.catalog, this.config);
    }

    public LoginResponse login(PluginAuthenticationProvider.AbstractPluginToken token, HttpServletResponse response)
            throws AuthenticationException {
        Authentication authenticatedUser = this.authenticationProvider().authenticate(token);
        if (authenticatedUser == null || !authenticatedUser.isAuthenticated() || authenticatedUser.getDetails() == null) {
            throw new BadCredentialsException("Username or password is incorrect");
        }

        AuthToken persistedToken = this.generateToken((User) authenticatedUser.getDetails());

        // Add the token as a cookie
        Cookie cookie = new Cookie(this.getCookieName(), persistedToken.getToken());
        cookie.setMaxAge((int) this.tokenDuration);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setAttribute("SameSite", "None");
        response.addCookie(cookie);

        return new LoginResponse(persistedToken.getToken(), this.tokenDuration);
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
        AuthToken token = new AuthToken(generateToken(), details, LocalDateTime.now().plusSeconds(this.tokenDuration));
        return this.repository.authTokenRepo.save(token);
    }

    public String getCookieName() {
        return cookieName;
    }
}
