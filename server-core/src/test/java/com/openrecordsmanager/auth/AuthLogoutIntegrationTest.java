package com.openrecordsmanager.auth;

import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.auth.dto.TokenPair;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthLogoutIntegrationTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        com.openrecordsmanager.database.SqliteTestSupport.registerPrimaryMemoryDatabase(registry, AuthLogoutIntegrationTest.class);
        registry.add(BuiltinConfigs.PLUGINS_SKIP_SYNC.key(), () -> "true");
        registry.add(BuiltinConfigs.COOKIE_SECURE.key(), () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataRepository repository;

    @Autowired
    private AuthService authService;

    @Autowired
    private TestAuthTokens testAuthTokens;

    @Test
    void logoutBumpsEpochAndBlocksFurtherUse() throws Exception {
        User user = this.repository.userRepo.findByUsername("admin").orElseThrow();
        TokenPair pair = this.testAuthTokens.tokenPairFor(user);

        this.mockMvc.perform(
                        post("/api/auth/logout")
                                .header("Authorization", "Bearer " + pair.accessToken())
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());

        this.mockMvc.perform(
                        get("/api/user/me")
                                .header("Authorization", "Bearer " + pair.accessToken())
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isUnauthorized());

        this.mockMvc.perform(
                        post("/api/auth/refresh")
                                .header("Authorization", "Bearer " + pair.refreshToken())
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutClearsAuthCookiesForWebClient() throws Exception {
        TokenPair pair = this.testAuthTokens.tokenPairFor("admin");

        this.mockMvc.perform(
                        post("/api/auth/logout")
                                .header("X-Client-Platform", "Web-Client")
                                .cookie(new Cookie(this.authService.getCookieName(), pair.accessToken()))
                                .cookie(new Cookie(this.authService.getRefreshCookieName(), pair.refreshToken()))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge(this.authService.getCookieName(), 0))
                .andExpect(cookie().maxAge(this.authService.getRefreshCookieName(), 0));
    }
}
