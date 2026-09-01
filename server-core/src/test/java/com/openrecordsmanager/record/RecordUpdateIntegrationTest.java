package com.openrecordsmanager.record;

import com.jayway.jsonpath.JsonPath;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.template.property.PropertyType;
import com.openrecordsmanager.api.template.recordtype.SecurityFilterUsage;
import com.openrecordsmanager.audit.AuditPolicyService;
import com.openrecordsmanager.auth.AuthService;
import com.openrecordsmanager.auth.entity.AuthToken;
import com.openrecordsmanager.auth.entity.AuthTokenRepository;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.api.builtin.BuiltinProperties;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.recordtype.RecordType;
import com.openrecordsmanager.recordtype.RecordTypeProperty;
import com.openrecordsmanager.user.User;
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

import java.time.Instant;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RecordUpdateIntegrationTest {

    private static final ResourceIdentifier TEST_RECORD_TYPE = ResourceIdentifier.valueOf("test:update_record_type");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add(BuiltinConfigs.PLUGINS_SKIP_SYNC.key(), () -> "true");
        registry.add(BuiltinConfigs.COOKIE_SECURE.key(), () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthTokenRepository tokenRepository;

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
    }

    private String adminBearerToken() {
        User admin = this.repository.userRepo.findByUsername("admin").orElseThrow();
        AuthToken token = new AuthToken(AuthService.generateToken(), admin, Instant.now().plusSeconds(3600));
        this.tokenRepository.saveAndFlush(token);
        return token.getToken();
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
}
