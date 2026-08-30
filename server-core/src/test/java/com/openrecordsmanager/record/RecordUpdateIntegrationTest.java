package com.openrecordsmanager.record;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.template.recordtype.SecurityFilterUsage;
import com.openrecordsmanager.auth.AuthService;
import com.openrecordsmanager.auth.entity.AuthToken;
import com.openrecordsmanager.auth.entity.AuthTokenRepository;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.recordtype.RecordType;
import com.jayway.jsonpath.JsonPath;
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
import java.util.HashSet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RecordUpdateIntegrationTest {

    private static final ResourceIdentifier TEST_RECORD_TYPE = ResourceIdentifier.valueOf("test:update_record_type");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add(BuiltinConfigs.PLUGINS_SKIP_STARTUP_CHECK.key(), () -> "true");
        registry.add(BuiltinConfigs.COOKIE_SECURE.key(), () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthTokenRepository tokenRepository;

    @Autowired
    private DataRepository repository;

    @BeforeEach
    void setUpRecordType() {
        if (this.repository.recordTypeRepo.findById(TEST_RECORD_TYPE).isEmpty()) {
            RecordType recordType = new RecordType(
                    TEST_RECORD_TYPE,
                    "Test record type",
                    "Test record type for update integration test",
                    null,
                    null,
                    SecurityFilterUsage.SHOW_ALL,
                    new HashSet<>()
            );
            this.repository.recordTypeRepo.saveAndFlush(recordType);
        }
    }

    private String adminBearerToken() {
        com.openrecordsmanager.user.User admin = this.repository.userRepo.findByUsername("admin").orElseThrow();
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
                .andExpect(jsonPath("$.data.title").value("tba"))
                .andReturn();

        String recordId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.data.id");

        this.mockMvc.perform(
                        put("/api/records/" + recordId)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "Updated title"
                                        }
                                        """)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated title"));

        this.mockMvc.perform(
                        get("/api/records/" + recordId)
                                .header("Authorization", "Bearer " + token)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated title"));
    }
}
