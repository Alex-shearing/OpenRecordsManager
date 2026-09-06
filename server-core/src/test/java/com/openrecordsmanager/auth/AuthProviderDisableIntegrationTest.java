package com.openrecordsmanager.auth;

import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.auth.dto.TokenPair;
import com.openrecordsmanager.auth.entity.AuthProvider;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthProviderDisableIntegrationTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        com.openrecordsmanager.database.SqliteTestSupport.registerPrimaryMemoryDatabase(registry, AuthProviderDisableIntegrationTest.class);
        registry.add(BuiltinConfigs.PLUGINS_SKIP_SYNC.key(), () -> "true");
        registry.add(BuiltinConfigs.COOKIE_SECURE.key(), () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataRepository repository;

    @Autowired
    private TestAuthTokens testAuthTokens;

    private AuthProvider localProvider() {
        return this.repository.authProviderRepo.findAll().stream()
                .filter(provider -> "Local Authentication".equals(provider.getName()))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void disableProviderHidesFromPublicListRevokesTokensAndBlocksLogin() throws Exception {
        String adminToken = this.testAuthTokens.adminAccessToken();
        AuthProvider provider = this.localProvider();
        TokenPair adminSession = this.testAuthTokens.tokenPairFor("admin");

        this.mockMvc.perform(get("/api/auth/providers").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        this.mockMvc.perform(
                        put("/api/auth/providers/" + provider.getId())
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "enabled": false
                                        }
                                        """)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());

        this.mockMvc.perform(
                        get("/api/user/me")
                                .header("Authorization", "Bearer " + adminSession.accessToken())
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isUnauthorized());

        this.mockMvc.perform(
                        get("/api/user/me")
                                .header("Authorization", "Bearer " + adminToken)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isUnauthorized());

        this.mockMvc.perform(get("/api/auth/providers").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        this.mockMvc.perform(
                        post("/api/auth/login/" + provider.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "username": "admin",
                                          "password": "admin"
                                        }
                                        """)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isUnauthorized());

        // Need a fresh admin token for manage endpoint after revocation
        String manageToken = this.testAuthTokens.adminAccessToken();
        this.mockMvc.perform(
                        put("/api/auth/providers/" + provider.getId())
                                .header("Authorization", "Bearer " + manageToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "enabled": true
                                        }
                                        """)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());

        this.mockMvc.perform(get("/api/auth/providers").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        this.mockMvc.perform(
                        post("/api/auth/login/" + provider.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "username": "admin",
                                          "password": "admin"
                                        }
                                        """)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists());

        assertTrue(this.localProvider().isEnabled());
    }
}
