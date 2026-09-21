package com.openrecordsmanager.auth;

import com.openrecordsmanager.api.auth.PendingRedirectAuth;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.database.SqliteTestSupport;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PendingRedirectAuthCookieTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SqliteTestSupport.registerPrimaryMemoryDatabase(registry, PendingRedirectAuthCookieTest.class);
        registry.add(BuiltinConfigs.PLUGINS_SKIP_SYNC.key(), () -> "true");
        registry.add(BuiltinConfigs.COOKIE_SECURE.key(), () -> "false");
    }

    @Autowired
    private PendingRedirectAuthCookie cookieStore;

    @Test
    void roundTripsPendingStateAndClearsCookie() {
        UUID providerId = UUID.randomUUID();
        PendingRedirectAuth pending = new PendingRedirectAuth(
                providerId,
                "state-value",
                Map.of("nonce", "nonce-value", "code_verifier", "verifier")
        );

        MockHttpServletResponse response = new MockHttpServletResponse();
        this.cookieStore.store(response, pending, "/records");

        Cookie setCookie = response.getCookie(PendingRedirectAuthCookie.COOKIE_NAME);
        assertNotNull(setCookie);
        assertTrue(setCookie.isHttpOnly());
        assertEquals("/api/auth", setCookie.getPath());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(setCookie);

        MockHttpServletResponse takeResponse = new MockHttpServletResponse();
        PendingRedirectAuthCookie.Loaded loaded = this.cookieStore.take(request, takeResponse, providerId);
        assertNotNull(loaded);
        assertEquals("state-value", loaded.pending().state());
        assertEquals("nonce-value", loaded.pending().attributes().get("nonce"));
        assertEquals("/records", loaded.returnTo());

        Cookie cleared = takeResponse.getCookie(PendingRedirectAuthCookie.COOKIE_NAME);
        assertNotNull(cleared);
        assertEquals(0, cleared.getMaxAge());
    }

    @Test
    void rejectsProviderMismatch() {
        UUID providerId = UUID.randomUUID();
        PendingRedirectAuth pending = new PendingRedirectAuth(providerId, "state", Map.of());

        MockHttpServletResponse response = new MockHttpServletResponse();
        this.cookieStore.store(response, pending, null);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(response.getCookie(PendingRedirectAuthCookie.COOKIE_NAME));

        assertNull(this.cookieStore.take(request, new MockHttpServletResponse(), UUID.randomUUID()));
    }
}
