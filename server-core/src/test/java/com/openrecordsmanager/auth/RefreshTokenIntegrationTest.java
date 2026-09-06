package com.openrecordsmanager.auth;

import com.jayway.jsonpath.JsonPath;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.auth.dto.TokenPair;
import com.openrecordsmanager.auth.entity.AuthProvider;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.database.SqliteTestSupport;
import com.openrecordsmanager.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RefreshTokenIntegrationTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SqliteTestSupport.registerPrimaryMemoryDatabase(registry, RefreshTokenIntegrationTest.class);
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
    void loginReturnsTokenPairAndRefreshIssuesNewAccessToken() throws Exception {
        AuthProvider provider = this.localProvider();

        MvcResult loginResult = this.mockMvc.perform(
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
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.sessionMode").value("NORMAL"))
                .andReturn();

        String body = loginResult.getResponse().getContentAsString();
        String accessToken = JsonPath.read(body, "$.data.accessToken");
        String refreshToken = JsonPath.read(body, "$.data.refreshToken");

        this.mockMvc.perform(
                        get("/api/user/me")
                                .header("Authorization", "Bearer " + accessToken)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin"));

        MvcResult refreshResult = this.mockMvc.perform(
                        post("/api/auth/refresh")
                                .header("Authorization", "Bearer " + refreshToken)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andReturn();

        String refreshedAccess = JsonPath.read(refreshResult.getResponse().getContentAsString(), "$.data.accessToken");
        assertNotEquals(accessToken, refreshedAccess);

        this.mockMvc.perform(
                        get("/api/user/me")
                                .header("Authorization", "Bearer " + refreshedAccess)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());
    }

    @Test
    void refreshTokenCannotAuthenticateApiRequests() throws Exception {
        TokenPair pair = this.testAuthTokens.tokenPairFor("admin");

        this.mockMvc.perform(
                        get("/api/user/me")
                                .header("Authorization", "Bearer " + pair.refreshToken())
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshAfterEpochBumpIsRejected() throws Exception {
        User admin = this.repository.userRepo.findByUsername("admin").orElseThrow();
        TokenPair pair = this.testAuthTokens.tokenPairFor(admin);

        admin.bumpSessionEpoch();
        this.repository.userRepo.saveAndFlush(admin);

        this.mockMvc.perform(
                        post("/api/auth/refresh")
                                .header("Authorization", "Bearer " + pair.refreshToken())
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isUnauthorized());
    }
}
