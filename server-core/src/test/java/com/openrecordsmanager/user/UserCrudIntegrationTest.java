package com.openrecordsmanager.user;

import com.jayway.jsonpath.JsonPath;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.template.list.IListElement;
import com.openrecordsmanager.api.template.property.PropertyType;
import com.openrecordsmanager.auth.TestAuthTokens;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.database.SqliteTestSupport;
import com.openrecordsmanager.list.ListElement;
import com.openrecordsmanager.list.ListType;
import com.openrecordsmanager.property.ObjectProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserCrudIntegrationTest {

    private static final ResourceIdentifier LIST_ID = ResourceIdentifier.valueOf("test:user_list_prop_list");
    private static final ResourceIdentifier ELEMENT_SECRET = ResourceIdentifier.valueOf("test:user_list_secret");
    private static final ResourceIdentifier ELEMENT_TOP_SECRET = ResourceIdentifier.valueOf("test:user_list_top_secret");
    private static final ResourceIdentifier LIST_ITEM_PROP = ResourceIdentifier.valueOf("test:user_list_item_prop");
    private static final ResourceIdentifier LIST_MULTI_PROP = ResourceIdentifier.valueOf("test:user_list_multi_prop");

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

    @BeforeEach
    void setUpListProperties() {
        ListType listType = this.repository.listTypeRepo.findById(LIST_ID).orElseGet(() ->
                this.repository.listTypeRepo.saveAndFlush(new ListType(LIST_ID, "User list prop list"))
        );

        this.repository.listElementRepo.findById(ELEMENT_SECRET).orElseGet(() ->
                this.repository.listElementRepo.saveAndFlush(
                        new ListElement(ELEMENT_SECRET, listType, "Secret", "", 1, null, Set.of())
                )
        );
        this.repository.listElementRepo.findById(ELEMENT_TOP_SECRET).orElseGet(() ->
                this.repository.listElementRepo.saveAndFlush(
                        new ListElement(ELEMENT_TOP_SECRET, listType, "Top Secret", "", 2, null, Set.of())
                )
        );

        if (this.repository.objectPropertyRepo.findById(LIST_ITEM_PROP).isEmpty()) {
            ObjectProperty<IListElement> itemProp = new ObjectProperty<>(
                    LIST_ITEM_PROP,
                    "User list item",
                    "User list item",
                    PropertyType.LIST_ITEM,
                    listType,
                    null,
                    null,
                    null,
                    false
            );
            this.repository.objectPropertyRepo.saveAndFlush(itemProp);
        }

        if (this.repository.objectPropertyRepo.findById(LIST_MULTI_PROP).isEmpty()) {
            ObjectProperty<Collection<IListElement>> multiProp = new ObjectProperty<>(
                    LIST_MULTI_PROP,
                    "User list multiple",
                    "User list multiple",
                    PropertyType.LIST_MULTIPLE,
                    listType,
                    null,
                    null,
                    null,
                    false
            );
            this.repository.objectPropertyRepo.saveAndFlush(multiProp);
        }
    }

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
    void setAndGetListItemProperties() throws Exception {
        String token = this.adminBearerToken();
        String username = "list_prop_user_" + UUID.randomUUID().toString().substring(0, 8);

        MvcResult createResult = this.mockMvc.perform(
                        post("/api/user")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "username": "%s",
                                          "authProvider": null,
                                          "properties": {
                                            "%s": "%s",
                                            "%s": ["%s"]
                                          }
                                        }
                                        """.formatted(
                                        username,
                                        LIST_ITEM_PROP,
                                        ELEMENT_SECRET,
                                        LIST_MULTI_PROP,
                                        ELEMENT_TOP_SECRET
                                ))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.properties['" + LIST_ITEM_PROP + "']").value(ELEMENT_SECRET.toString()))
                .andExpect(jsonPath("$.data.properties['" + LIST_MULTI_PROP + "'][0]").value(ELEMENT_TOP_SECRET.toString()))
                .andReturn();

        String userId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.data.id");

        this.mockMvc.perform(
                        put("/api/user/" + userId)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "properties": {
                                            "%s": "%s",
                                            "%s": ["%s", "%s"]
                                          }
                                        }
                                        """.formatted(
                                        LIST_ITEM_PROP,
                                        ELEMENT_TOP_SECRET,
                                        LIST_MULTI_PROP,
                                        ELEMENT_SECRET,
                                        ELEMENT_TOP_SECRET
                                ))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.properties['" + LIST_ITEM_PROP + "']").value(ELEMENT_TOP_SECRET.toString()))
                .andExpect(jsonPath("$.data.properties['" + LIST_MULTI_PROP + "'][0]").value(ELEMENT_SECRET.toString()))
                .andExpect(jsonPath("$.data.properties['" + LIST_MULTI_PROP + "'][1]").value(ELEMENT_TOP_SECRET.toString()));

        this.mockMvc.perform(
                        get("/api/user/" + userId)
                                .header("Authorization", "Bearer " + token)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.properties['" + LIST_ITEM_PROP + "']").value(ELEMENT_TOP_SECRET.toString()))
                .andExpect(jsonPath("$.data.properties['" + LIST_MULTI_PROP + "'][0]").value(ELEMENT_SECRET.toString()))
                .andExpect(jsonPath("$.data.properties['" + LIST_MULTI_PROP + "'][1]").value(ELEMENT_TOP_SECRET.toString()));
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
