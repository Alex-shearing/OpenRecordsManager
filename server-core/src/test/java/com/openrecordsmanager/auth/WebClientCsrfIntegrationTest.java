package com.openrecordsmanager.auth;

import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.auth.entity.AuthToken;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.user.User;
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

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WebClientCsrfIntegrationTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add(BuiltinConfigs.PLUGINS_SKIP_SYNC.key(), () -> "true");
        registry.add(BuiltinConfigs.COOKIE_SECURE.key(), () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataRepository repository;

    @Autowired
    private AuthService authService;

    @Test
    void cookieAuthMutatingRequestRequiresCsrfToken() throws Exception {
        User user = this.repository.userRepo.findByUsername("admin").orElseThrow();
        AuthToken token = new AuthToken(AuthService.generateToken(), user, Instant.now().plusSeconds(3600));
        this.repository.authTokenRepo.saveAndFlush(token);

        var authCookie = new Cookie(this.authService.getCookieName(), token.getToken());

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
                .andExpect(header().exists("X-CSRF-TOKEN"))
                .andReturn();

        String csrfToken = bootstrap.getResponse().getHeader("X-CSRF-TOKEN");
        assertNotNull(csrfToken);
    }

    @Test
    void crossOriginClientCanUseCsrfTokenFromResponseHeader() throws Exception {
        User user = this.repository.userRepo.findByUsername("admin").orElseThrow();
        AuthToken token = new AuthToken(AuthService.generateToken(), user, Instant.now().plusSeconds(3600));
        this.repository.authTokenRepo.saveAndFlush(token);

        var authCookie = new jakarta.servlet.http.Cookie(this.authService.getCookieName(), token.getToken());

        MvcResult bootstrap = this.mockMvc.perform(
                        get("/api/user/me")
                                .header("X-Client-Platform", "Web-Client")
                                .cookie(authCookie)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(header().exists("X-CSRF-TOKEN"))
                .andReturn();

        String csrfToken = bootstrap.getResponse().getHeader("X-CSRF-TOKEN");
        assertNotNull(csrfToken);
    }
}
