package com.openrecordsmanager.auth;

import com.jayway.jsonpath.JsonPath;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
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

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthProviderSettingsIntegrationTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SqliteTestSupport.registerPrimaryMemoryDatabase(registry, AuthProviderSettingsIntegrationTest.class);
        registry.add(BuiltinConfigs.PLUGINS_SKIP_SYNC.key(), () -> "true");
        registry.add(BuiltinConfigs.COOKIE_SECURE.key(), () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestAuthTokens testAuthTokens;

    @Test
    void listProviderTypesIncludesLocalAndOidcWithSchemas() throws Exception {
        String adminToken = this.testAuthTokens.adminAccessToken();

        this.mockMvc.perform(
                        get("/api/auth/providers/types")
                                .header("Authorization", "Bearer " + adminToken)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].type.id", hasItem("auth_local:local_auth")))
                .andExpect(jsonPath("$.data[*].type.id", hasItem("auth_oidc:oidc_auth")))
                .andExpect(jsonPath("$.data[?(@.type.id == 'auth_local:local_auth')].settingsSchema.properties").value(hasItem(nullValue())))
                .andExpect(jsonPath("$.data[?(@.type.id == 'auth_oidc:oidc_auth')].settingsSchema.properties.secret.writeOnly")
                        .value(hasItem(true)));
    }

    @Test
    void createOidcProviderRejectsInvalidSettings() throws Exception {
        String adminToken = this.testAuthTokens.adminAccessToken();

        this.mockMvc.perform(
                        put("/api/auth/providers")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "Broken OIDC",
                                          "type": {
                                            "id": "auth_oidc:oidc_auth",
                                            "type": "redirect_auth_provider"
                                          },
                                          "settings": {
                                            "clientId": "x"
                                          }
                                        }
                                        """)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAndUpdateOidcProviderRedactsSecretAndPreservesOnBlankUpdate() throws Exception {
        String adminToken = this.testAuthTokens.adminAccessToken();

        MvcResult created = this.mockMvc.perform(
                        put("/api/auth/providers")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "Test OIDC",
                                          "type": {
                                            "id": "auth_oidc:oidc_auth",
                                            "type": "redirect_auth_provider"
                                          },
                                          "settings": {
                                            "clientId": "orm-client",
                                            "secret": "super-secret-value",
                                            "uri": "http://localhost:9000/application/o/orm/",
                                            "scope": "openid profile",
                                            "usernameClaim": "preferred_username"
                                          }
                                        }
                                        """)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.settings.clientId").value("orm-client"))
                .andExpect(jsonPath("$.data.settings.secret").doesNotExist())
                .andExpect(jsonPath("$.data.settings.uri").value("http://localhost:9000/application/o/orm/"))
                .andReturn();

        String body = created.getResponse().getContentAsString();
        assertFalse(body.contains("super-secret-value"));

        String id = JsonPath.read(body, "$.data.id");

        this.mockMvc.perform(
                        put("/api/auth/providers/" + id)
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "settings": {
                                            "clientId": "orm-client-updated",
                                            "secret": "",
                                            "uri": "http://localhost:9000/application/o/orm/",
                                            "scope": "openid profile email",
                                            "usernameClaim": "preferred_username"
                                          }
                                        }
                                        """)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settings.clientId").value("orm-client-updated"))
                .andExpect(jsonPath("$.data.settings.secret").doesNotExist());

        this.mockMvc.perform(
                        get("/api/auth/providers/" + id)
                                .header("Authorization", "Bearer " + adminToken)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settings.clientId").value("orm-client-updated"))
                .andExpect(jsonPath("$.data.settings.secret").doesNotExist());
    }
}
