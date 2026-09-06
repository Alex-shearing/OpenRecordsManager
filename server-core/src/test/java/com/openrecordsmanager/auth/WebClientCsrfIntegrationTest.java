package com.openrecordsmanager.auth;

import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.database.SqliteTestSupport;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WebClientCsrfIntegrationTest {

    private static final String CSRF_HEADER = "X-CSRF-TOKEN";
    private static final String CSRF_COOKIE = "XSRF-TOKEN";

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SqliteTestSupport.registerPrimaryMemoryDatabase(registry, WebClientCsrfIntegrationTest.class);
        registry.add(BuiltinConfigs.PLUGINS_SKIP_SYNC.key(), () -> "true");
        registry.add(BuiltinConfigs.COOKIE_SECURE.key(), () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private TestAuthTokens testAuthTokens;

    @Test
    void cookieAuthMutatingRequestRequiresCsrfToken() throws Exception {
        String accessToken = this.testAuthTokens.adminAccessToken();

        var authCookie = new Cookie(this.authService.getCookieName(), accessToken);

        this.mockMvc.perform(
                        put("/api/config/")
                                .header("X-Client-Platform", "Web-Client")
                                .cookie(authCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isForbidden());

        MvcResult bootstrap = this.mockMvc.perform(
                        get("/api/user/me")
                                .header("X-Client-Platform", "Web-Client")
                                .cookie(authCookie)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(header().exists(CSRF_HEADER))
                .andReturn();

        String csrfToken = bootstrap.getResponse().getHeader(CSRF_HEADER);
        assertNotNull(csrfToken);

        Cookie csrfCookie = bootstrap.getResponse().getCookie(CSRF_COOKIE);
        assertNotNull(csrfCookie);
        assertEquals(csrfToken, csrfCookie.getValue());

        this.mockMvc.perform(
                        put("/api/config/")
                                .header("X-Client-Platform", "Web-Client")
                                .header(CSRF_HEADER, csrfToken)
                                .cookie(authCookie, csrfCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());
    }

    @Test
    void crossOriginClientCanUseCsrfTokenFromResponseHeader() throws Exception {
        String accessToken = this.testAuthTokens.adminAccessToken();

        var authCookie = new Cookie(this.authService.getCookieName(), accessToken);

        MvcResult bootstrap = this.mockMvc.perform(
                        get("/api/user/me")
                                .header("X-Client-Platform", "Web-Client")
                                .cookie(authCookie)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(header().exists(CSRF_HEADER))
                .andReturn();

        String csrfToken = bootstrap.getResponse().getHeader(CSRF_HEADER);
        assertNotNull(csrfToken);

        Cookie csrfCookie = bootstrap.getResponse().getCookie(CSRF_COOKIE);
        assertNotNull(csrfCookie);
        assertEquals(csrfToken, csrfCookie.getValue(), "X-CSRF-TOKEN header must match XSRF-TOKEN cookie");

        this.mockMvc.perform(
                        put("/api/config/")
                                .header("X-Client-Platform", "Web-Client")
                                .header(CSRF_HEADER, csrfToken)
                                .cookie(authCookie, csrfCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());
    }

    @Test
    void authenticatedRequestsDoNotRotateCsrfToken() throws Exception {
        String accessToken = this.testAuthTokens.adminAccessToken();
        var authCookie = new Cookie(this.authService.getCookieName(), accessToken);

        MvcResult first = this.mockMvc.perform(
                        get("/api/user/me")
                                .header("X-Client-Platform", "Web-Client")
                                .cookie(authCookie)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(header().exists(CSRF_HEADER))
                .andReturn();

        String firstHeader = first.getResponse().getHeader(CSRF_HEADER);
        Cookie firstCookie = first.getResponse().getCookie(CSRF_COOKIE);
        assertNotNull(firstHeader);
        assertNotNull(firstCookie);
        assertEquals(firstHeader, firstCookie.getValue());

        MvcResult second = this.mockMvc.perform(
                        get("/api/user/me")
                                .header("X-Client-Platform", "Web-Client")
                                .cookie(authCookie, firstCookie)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(header().string(CSRF_HEADER, firstHeader))
                .andReturn();

        Cookie secondCookie = second.getResponse().getCookie(CSRF_COOKIE);
        if (secondCookie != null) {
            assertEquals(firstHeader, secondCookie.getValue());
        }

        this.mockMvc.perform(
                        put("/api/config/")
                                .header("X-Client-Platform", "Web-Client")
                                .header(CSRF_HEADER, firstHeader)
                                .cookie(authCookie, firstCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());
    }
}
