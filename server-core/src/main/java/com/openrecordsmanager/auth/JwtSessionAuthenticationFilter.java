package com.openrecordsmanager.auth;

import com.openrecordsmanager.auth.dto.SessionMode;
import com.openrecordsmanager.user.User;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtSessionAuthenticationFilter extends OncePerRequestFilter {

    public static final String SESSION_MODE_HEADER = "X-ORM-Session-Mode";

    private final JwtSessionService jwtSessionService;
    private final AuthService authService;

    public JwtSessionAuthenticationFilter(
            JwtSessionService jwtSessionService,
            AuthService authService
    ) {
        this.jwtSessionService = jwtSessionService;
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String tokenValue = this.authService.extractTokenFromRequest(request, this.authService.getCookieName());

        if (tokenValue != null) {
            try {
                Claims claims = this.jwtSessionService.verifyAccessToken(tokenValue);
                User user = this.jwtSessionService.resolveUser(claims);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                SessionMode mode = this.jwtSessionService.sessionModeFromClaims(claims);
                response.setHeader(SESSION_MODE_HEADER, mode.name());
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
