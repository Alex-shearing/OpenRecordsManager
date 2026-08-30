package com.openrecordsmanager.auth;

import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.auth.entity.AuthToken;
import com.openrecordsmanager.auth.entity.AuthTokenRepository;
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

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthLogoutIntegrationTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add(BuiltinConfigs.PLUGINS_SKIP_STARTUP_CHECK.key(), () -> "true");
        registry.add(BuiltinConfigs.COOKIE_SECURE.key(), () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthTokenRepository tokenRepository;

    @Autowired
    private DataRepository repository;

    @Autowired
    private AuthService authService;

    @Test
    void logoutDeletesBearerTokenAndBlocksFurtherUse() throws Exception {
        User user = this.repository.userRepo.findByUsername("admin").orElseThrow();
        AuthToken token = new AuthToken(AuthService.generateToken(), user, Instant.now().plusSeconds(3600));
        this.tokenRepository.saveAndFlush(token);

        this.mockMvc.perform(
                        post("/api/auth/logout")
                                .header("Authorization", "Bearer " + token.getToken())
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());

        assertFalse(this.tokenRepository.existsById(token.getToken()));

        this.mockMvc.perform(
                        get("/api/user/me")
                                .header("Authorization", "Bearer " + token.getToken())
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutClearsAuthCookieForWebClient() throws Exception {
        User user = this.repository.userRepo.findByUsername("admin").orElseThrow();
        AuthToken token = new AuthToken(AuthService.generateToken(), user, Instant.now().plusSeconds(3600));
        this.tokenRepository.saveAndFlush(token);

        this.mockMvc.perform(
                        post("/api/auth/logout")
                                .header("X-Client-Platform", "Web-Client")
                                .cookie(new Cookie(this.authService.getCookieName(), token.getToken()))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge(this.authService.getCookieName(), 0));

        assertFalse(this.tokenRepository.existsById(token.getToken()));
    }
}
