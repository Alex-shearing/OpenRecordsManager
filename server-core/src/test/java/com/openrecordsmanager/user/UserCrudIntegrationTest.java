package com.openrecordsmanager.user;

import com.jayway.jsonpath.JsonPath;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.auth.TestAuthTokens;
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
class UserCrudIntegrationTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SqliteTestSupport.registerPrimaryMemoryDatabase(registry, UserCrudIntegrationTest.class);
        registry.add(BuiltinConfigs.PLUGINS_SKIP_SYNC.key(), () -> "true");
        registry.add(BuiltinConfigs.COOKIE_SECURE.key(), () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestAuthTokens testAuthTokens;

    @Autowired
    private DataRepository repository;

    private String adminBearerToken() {
        return this.testAuthTokens.adminAccessToken();
    }

    @Test
    void createGetAndUpdateUser() throws Exception {
        String token = this.adminBearerToken();
        String username = "integration_user_" + UUID.randomUUID().toString().substring(0, 8);

        MvcResult createResult = this.mockMvc.perform(
                        post("/api/user")
                                .header("Authorization", "Bearer " + token)
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
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String userId = JsonPath.read(responseBody, "$.data.id");

        this.mockMvc.perform(
                        get("/api/user/" + userId)
                                .header("Authorization", "Bearer " + token)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(username));

        String updatedUsername = username + "_updated";
        this.mockMvc.perform(
                        put("/api/user/" + userId)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "username": "%s"
                                        }
                                        """.formatted(updatedUsername))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(updatedUsername));
    }

    @Test
    void createDuplicateUsernameReturnsConflict() throws Exception {
        String token = this.adminBearerToken();
        String username = "duplicate_user_" + UUID.randomUUID().toString().substring(0, 8);
        String body = """
                {
                  "username": "%s",
                  "authProvider": null,
                  "properties": {}
                }
                """.formatted(username);

        this.mockMvc.perform(
                        post("/api/user")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());

        this.mockMvc.perform(
                        post("/api/user")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isConflict());
    }

    @Test
    void getUnknownUserReturnsNotFound() throws Exception {
        String token = this.adminBearerToken();

        this.mockMvc.perform(
                        get("/api/user/" + UUID.randomUUID())
                                .header("Authorization", "Bearer " + token)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound());
    }
}
