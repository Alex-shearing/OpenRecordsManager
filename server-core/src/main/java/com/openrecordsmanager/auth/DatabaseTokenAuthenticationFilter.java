package com.openrecordsmanager.auth;

import com.openrecordsmanager.auth.entity.AuthTokenRepository;
import com.openrecordsmanager.user.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

public class DatabaseTokenAuthenticationFilter extends OncePerRequestFilter {

    private final AuthTokenRepository tokenRepository;
    private final AuthService authService;

    public DatabaseTokenAuthenticationFilter(AuthTokenRepository tokenRepository, AuthService authService) {
        this.tokenRepository = tokenRepository;
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String tokenValue = this.extractTokenFromRequest(request);

        if (tokenValue != null) {
            this.tokenRepository.findById(tokenValue).ifPresent(userToken -> {
                if (!userToken.isExpired()) {
                    User user = userToken.getUser();

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Establish security context for the current thread/session
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            });
        }

        filterChain.doFilter(request, response);
    }

    private @Nullable String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null) {
            if (authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
            return null;
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            Optional<Cookie> authCookie = Arrays.stream(cookies)
                    .filter(cookie -> cookie.getName().equals(this.authService.getCookieName()))
                    .findFirst();

            if (authCookie.isPresent() && !authCookie.get().getValue().isBlank()) {
                return authCookie.get().getValue();
            }
        }

        return null;
    }
}