package com.openrecordsmanager.user;

import com.jayway.jsonpath.JsonPath;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.auth.TestAuthTokens;
import com.openrecordsmanager.auth.dto.TokenPair;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.database.SqliteTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserDisableIntegrationTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SqliteTestSupport.registerPrimaryMemoryDatabase(registry, UserDisableIntegrationTest.class);
        registry.add(BuiltinConfigs.PLUGINS_SKIP_SYNC.key(), () -> "true");
        registry.add(BuiltinConfigs.COOKIE_SECURE.key(), () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataRepository repository;

    @Autowired
    private TestAuthTokens testAuthTokens;

    @Test
    void disableUserRevokesTokensAndBlocksLogin() throws Exception {
        String adminToken = this.testAuthTokens.adminAccessToken();
        String username = "disable_user_" + UUID.randomUUID().toString().substring(0, 8);

        MvcResult createResult = this.mockMvc.perform(
                        post("/api/user")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "username": "%s",
                                          "authProvider": null,
                                          "properties": {}
                                        }
                                        """.formatted(username))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andReturn();

        String userId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.data.id");
        User user = this.repository.userRepo.findById(UUID.fromString(userId)).orElseThrow();
        TokenPair userPair = this.testAuthTokens.tokenPairFor(user);

        this.mockMvc.perform(
                        put("/api/user/" + userId)
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "enabled": false
                                        }
                                        """)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));

        this.mockMvc.perform(
                        get("/api/user/me")
                                .header("Authorization", "Bearer " + userPair.accessToken())
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isUnauthorized());

        this.mockMvc.perform(
                        put("/api/user/" + userId)
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "enabled": true
                                        }
                                        """)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    @Test
    void cannotDisableOwnAccount() throws Exception {
        String adminToken = this.testAuthTokens.adminAccessToken();
        User admin = this.repository.userRepo.findByUsername("admin").orElseThrow();

        this.mockMvc.perform(
                        put("/api/user/" + admin.getId())
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "enabled": false
                                        }
                                        """)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isConflict());
    }
}
