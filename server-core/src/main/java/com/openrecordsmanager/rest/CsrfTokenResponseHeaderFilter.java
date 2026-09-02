package com.openrecordsmanager.rest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * Exposes the CSRF token in a response header so cross-origin web clients can read it.
 */
@Component
public class CsrfTokenResponseHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        CsrfToken csrfToken = resolveCsrfToken(request);
        if (csrfToken != null) {
            response.setHeader(csrfToken.getHeaderName(), csrfToken.getToken());
        }

        filterChain.doFilter(request, response);
    }

    private static @Nullable CsrfToken resolveCsrfToken(HttpServletRequest request) {
        CsrfToken token = asCsrfToken(request.getAttribute(CsrfToken.class.getName()));
        if (token != null) {
            return token;
        }

        return asCsrfToken(request.getAttribute("_csrf"));
    }

    private static @Nullable CsrfToken asCsrfToken(@Nullable Object attribute) {
        if (attribute instanceof CsrfToken csrfToken) {
            return csrfToken;
        }

        if (attribute instanceof Supplier<?> supplier) {
            Object supplied = supplier.get();
            if (supplied instanceof CsrfToken csrfToken) {
                return csrfToken;
            }
        }

        return null;
    }
}
