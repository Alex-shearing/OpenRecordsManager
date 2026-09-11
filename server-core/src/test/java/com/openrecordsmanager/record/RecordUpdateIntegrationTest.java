package com.openrecordsmanager.record;

import com.jayway.jsonpath.JsonPath;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.builtin.BuiltinProperties;
import com.openrecordsmanager.api.template.list.IListElement;
import com.openrecordsmanager.api.template.property.PropertyType;
import com.openrecordsmanager.api.template.recordtype.SecurityFilterUsage;
import com.openrecordsmanager.audit.AuditPolicyService;
import com.openrecordsmanager.auth.TestAuthTokens;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.list.ListElement;
import com.openrecordsmanager.list.ListType;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.recordtype.RecordType;
import com.openrecordsmanager.recordtype.RecordTypeProperty;
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
import java.util.LinkedHashSet;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RecordUpdateIntegrationTest {

    private static final ResourceIdentifier TEST_RECORD_TYPE = ResourceIdentifier.valueOf("test:update_record_type");
    private static final ResourceIdentifier LIST_ID = ResourceIdentifier.valueOf("test:record_list_prop_list");
    private static final ResourceIdentifier ELEMENT_A = ResourceIdentifier.valueOf("test:record_list_a");
    private static final ResourceIdentifier ELEMENT_B = ResourceIdentifier.valueOf("test:record_list_b");
    private static final ResourceIdentifier LIST_ITEM_PROP = ResourceIdentifier.valueOf("test:record_list_item_prop");
    private static final ResourceIdentifier LIST_MULTI_PROP = ResourceIdentifier.valueOf("test:record_list_multi_prop");
    private static final ResourceIdentifier LIST_RECORD_TYPE = ResourceIdentifier.valueOf("test:list_prop_record_type");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        com.openrecordsmanager.database.SqliteTestSupport.registerPrimaryMemoryDatabase(registry, RecordUpdateIntegrationTest.class);
        registry.add(BuiltinConfigs.PLUGINS_SKIP_SYNC.key(), () -> "true");
        registry.add(BuiltinConfigs.COOKIE_SECURE.key(), () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestAuthTokens testAuthTokens;

    @Autowired
    private DataRepository repository;

    @Autowired
    private AuditPolicyService auditPolicyService;

    @BeforeEach
    void setUpRecordType() {
        this.auditPolicyService.updatePolicy(AuditEntityType.RECORD, AuditOperation.CREATE, true, false);
        this.auditPolicyService.updatePolicy(AuditEntityType.RECORD, AuditOperation.UPDATE, true, false);

        if (this.repository.recordTypeRepo.findById(TEST_RECORD_TYPE).isEmpty()) {
            ObjectProperty<String> titleProperty = new ObjectProperty<>(
                    BuiltinProperties.TITLE_ID,
                    "Title",
                    "Title",
                    PropertyType.STRING
            );
            RecordType recordType = new RecordType(
                    TEST_RECORD_TYPE,
                    "Test record type",
                    "Test record type for update integration test",
                    null,
                    null,
                    SecurityFilterUsage.SHOW_ALL,
                    Set.of(new RecordTypeProperty<>(titleProperty, null))
            );
            this.repository.recordTypeRepo.saveAndFlush(recordType);
        }

        ListType listType = this.repository.listTypeRepo.findById(LIST_ID).orElseGet(() ->
                this.repository.listTypeRepo.saveAndFlush(new ListType(LIST_ID, "Record list prop list"))
        );

        this.repository.listElementRepo.findById(ELEMENT_A).orElseGet(() ->
                this.repository.listElementRepo.saveAndFlush(
                        new ListElement(ELEMENT_A, listType, "A", "", 1, null, Set.of())
                )
        );
        this.repository.listElementRepo.findById(ELEMENT_B).orElseGet(() ->
                this.repository.listElementRepo.saveAndFlush(
                        new ListElement(ELEMENT_B, listType, "B", "", 2, null, Set.of())
                )
        );

        ObjectProperty<IListElement> listItemProp = this.repository.objectPropertyRepo.findById(LIST_ITEM_PROP)
                .map(p -> (ObjectProperty<IListElement>) p)
                .orElseGet(() -> this.repository.objectPropertyRepo.saveAndFlush(new ObjectProperty<>(
                        LIST_ITEM_PROP,
                        "Record list item",
                        "Record list item",
                        PropertyType.LIST_ITEM,
                        listType,
                        null,
                        null,
                        null,
                        false
                )));

        ObjectProperty<Collection<IListElement>> listMultiProp = this.repository.objectPropertyRepo.findById(LIST_MULTI_PROP)
                .map(p -> (ObjectProperty<Collection<IListElement>>) p)
                .orElseGet(() -> this.repository.objectPropertyRepo.saveAndFlush(new ObjectProperty<>(
                        LIST_MULTI_PROP,
                        "Record list multiple",
                        "Record list multiple",
                        PropertyType.LIST_MULTIPLE,
                        listType,
                        null,
                        null,
                        null,
                        false
                )));

        if (this.repository.recordTypeRepo.findById(LIST_RECORD_TYPE).isEmpty()) {
            ObjectProperty<String> titleProperty = this.repository.objectPropertyRepo.findById(BuiltinProperties.TITLE_ID)
                    .map(p -> (ObjectProperty<String>) p)
                    .orElseGet(() -> this.repository.objectPropertyRepo.saveAndFlush(new ObjectProperty<>(
                            BuiltinProperties.TITLE_ID,
                            "Title",
                            "Title",
                            PropertyType.STRING
                    )));

            Set<RecordTypeProperty<?>> properties = new LinkedHashSet<>();
            properties.add(new RecordTypeProperty<>(titleProperty, null));
            properties.add(new RecordTypeProperty<>(listItemProp, null));
            properties.add(new RecordTypeProperty<>(listMultiProp, null));

            RecordType recordType = new RecordType(
                    LIST_RECORD_TYPE,
                    "List prop record type",
                    "Record type with list properties",
                    null,
                    null,
                    SecurityFilterUsage.SHOW_ALL,
                    properties
            );
            this.repository.recordTypeRepo.saveAndFlush(recordType);
        }
    }

    private String adminBearerToken() {
        return this.testAuthTokens.adminAccessToken();
    }

    @Test
    void createAndUpdateRecordTitle() throws Exception {
        String token = this.adminBearerToken();

        MvcResult createResult = this.mockMvc.perform(
                        post("/api/records")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "type": "%s",
                                          "properties": {}
                                        }
                                        """.formatted(TEST_RECORD_TYPE))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.properties['builtin:title']").value("tba"))
                .andReturn();

        String recordId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.data.id");

        this.mockMvc.perform(
                        put("/api/records/" + recordId)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "properties": {
                                            "builtin:title": "Updated title"
                                          }
                                        }
                                        """)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.properties['builtin:title']").value("Updated title"));

        this.mockMvc.perform(
                        get("/api/records/" + recordId)
                                .header("Authorization", "Bearer " + token)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.properties['builtin:title']").value("Updated title"));
    }

    @Test
    void createAndUpdateRecordListItemProperties() throws Exception {
        String token = this.adminBearerToken();

        MvcResult createResult = this.mockMvc.perform(
                        post("/api/records")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "type": "%s",
                                          "properties": {
                                            "%s": "%s",
                                            "%s": ["%s"]
                                          }
                                        }
                                        """.formatted(
                                        LIST_RECORD_TYPE,
                                        LIST_ITEM_PROP,
                                        ELEMENT_A,
                                        LIST_MULTI_PROP,
                                        ELEMENT_B
                                ))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.properties['" + LIST_ITEM_PROP + "']").value(ELEMENT_A.toString()))
                .andExpect(jsonPath("$.data.properties['" + LIST_MULTI_PROP + "'][0]").value(ELEMENT_B.toString()))
                .andReturn();

        String recordId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.data.id");

        this.mockMvc.perform(
                        put("/api/records/" + recordId)
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
                                        ELEMENT_B,
                                        LIST_MULTI_PROP,
                                        ELEMENT_A,
                                        ELEMENT_B
                                ))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.properties['" + LIST_ITEM_PROP + "']").value(ELEMENT_B.toString()))
                .andExpect(jsonPath("$.data.properties['" + LIST_MULTI_PROP + "'][0]").value(ELEMENT_A.toString()))
                .andExpect(jsonPath("$.data.properties['" + LIST_MULTI_PROP + "'][1]").value(ELEMENT_B.toString()));

        this.mockMvc.perform(
                        get("/api/records/" + recordId)
                                .header("Authorization", "Bearer " + token)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.properties['" + LIST_ITEM_PROP + "']").value(ELEMENT_B.toString()))
                .andExpect(jsonPath("$.data.properties['" + LIST_MULTI_PROP + "'][0]").value(ELEMENT_A.toString()))
                .andExpect(jsonPath("$.data.properties['" + LIST_MULTI_PROP + "'][1]").value(ELEMENT_B.toString()));
    }
}
