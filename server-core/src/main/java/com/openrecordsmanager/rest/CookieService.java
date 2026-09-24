package com.openrecordsmanager.rest;

import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.config.ConfigService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import java.time.Duration;

/**
 * Shared helpers for HttpOnly auth cookies with SameSite tied to the Secure flag.
 */
@Component
public class CookieService {

    private final boolean cookieSecure;

    public CookieService(ConfigService config) {
        this.cookieSecure = config.getOrThrow(BuiltinConfigs.COOKIE_SECURE);
    }

    public @Nullable String read(HttpServletRequest request, String name) {
        Cookie cookie = WebUtils.getCookie(request, name);
        if (cookie == null) {
            return null;
        }
        String value = cookie.getValue();
        return StringUtils.isBlank(value) ? null : value;
    }

    public void write(
            HttpServletResponse response,
            String name,
            String value,
            long maxAgeSeconds,
            String path
    ) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .maxAge(Duration.ofSeconds(Math.max(0, maxAgeSeconds)))
                .httpOnly(true)
                .secure(this.cookieSecure)
                .path(path)
                .sameSite(this.cookieSecure ? "None" : "Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clear(HttpServletResponse response, String name, String path) {
        this.write(response, name, "", 0, path);
    }
}
