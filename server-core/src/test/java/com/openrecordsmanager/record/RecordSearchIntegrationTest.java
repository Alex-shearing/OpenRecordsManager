package com.openrecordsmanager.record;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.builtin.BuiltinPropertyIds;
import com.openrecordsmanager.api.template.property.PropertyType;
import com.openrecordsmanager.api.template.recordtype.SecurityFilterUsage;
import com.openrecordsmanager.audit.AuditPolicyService;
import com.openrecordsmanager.auth.TestAuthTokens;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.database.SqliteTestSupport;
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

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RecordSearchIntegrationTest {

    private static final ResourceIdentifier SEARCH_RECORD_TYPE = ResourceIdentifier.valueOf("test:search_record_type");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SqliteTestSupport.registerPrimaryMemoryDatabase(registry, RecordSearchIntegrationTest.class);
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
    void setUp() {
        this.auditPolicyService.updatePolicy(AuditEntityType.RECORD, AuditOperation.CREATE, true, false);
        this.auditPolicyService.updatePolicy(AuditEntityType.RECORD, AuditOperation.READ, true, false);

        ObjectProperty<String> titleProperty = this.repository.objectPropertyRepo.findById(BuiltinPropertyIds.TITLE)
                .map(p -> (ObjectProperty<String>) p)
                .orElseGet(() -> this.repository.objectPropertyRepo.saveAndFlush(new ObjectProperty<>(
                        BuiltinPropertyIds.TITLE,
                        "Title",
                        "Title",
                        PropertyType.STRING
                )));

        if (this.repository.recordTypeRepo.findById(SEARCH_RECORD_TYPE).isEmpty()) {
            RecordType recordType = new RecordType(
                    SEARCH_RECORD_TYPE,
                    "Search record type",
                    "Record type for search tests",
                    null,
                    null,
                    SecurityFilterUsage.SHOW_ALL,
                    Set.of(new RecordTypeProperty<>(titleProperty, null))
            );
            this.repository.recordTypeRepo.saveAndFlush(recordType);
        }
    }

    @Test
    void searchByDefaultQMatchesTitle() throws Exception {
        String token = this.testAuthTokens.adminAccessToken();

        this.mockMvc.perform(
                post("/api/records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "%s",
                                  "properties": {
                                    "builtin:title": "Alpha Search Target"
                                  }
                                }
                                """.formatted(SEARCH_RECORD_TYPE))
        ).andExpect(status().isOk());

        this.mockMvc.perform(
                post("/api/records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "%s",
                                  "properties": {
                                    "builtin:title": "Unrelated Document"
                                  }
                                }
                                """.formatted(SEARCH_RECORD_TYPE))
        ).andExpect(status().isOk());

        this.mockMvc.perform(
                post("/api/records/search")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "q": "Alpha Search",
                                  "type": "%s"
                                }
                                """.formatted(SEARCH_RECORD_TYPE))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].properties['builtin:title']").value("Alpha Search Target"));
    }

    @Test
    void searchByPropertyFilterEquals() throws Exception {
        String token = this.testAuthTokens.adminAccessToken();

        this.mockMvc.perform(
                post("/api/records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "%s",
                                  "properties": {
                                    "builtin:title": "Exact Filter Title"
                                  }
                                }
                                """.formatted(SEARCH_RECORD_TYPE))
        ).andExpect(status().isOk());

        this.mockMvc.perform(
                post("/api/records/search")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "filters": [
                                    {
                                      "field": "builtin:title",
                                      "op": "EQ",
                                      "value": "Exact Filter Title"
                                    }
                                  ]
                                }
                                """)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].properties['builtin:title']").value("Exact Filter Title"));
    }
}
