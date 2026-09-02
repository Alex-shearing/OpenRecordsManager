package com.openrecordsmanager.audit;

import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.auth.AuthService;
import com.openrecordsmanager.auth.entity.AuthToken;
import com.openrecordsmanager.auth.entity.AuthTokenRepository;
import com.openrecordsmanager.database.DataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuditStatusIntegrationTest {

    private static Path spoolDirectory;

    @DynamicPropertySource
    static void auditProperties(DynamicPropertyRegistry registry) {
        spoolDirectory = Path.of("build/test-audit-status-" + UUID.randomUUID());
        registry.add(BuiltinConfigs.AUDIT_SPOOL_DIRECTORY.key(), () -> spoolDirectory.toString());
        registry.add(BuiltinConfigs.AUDIT_SPOOL_DRAIN_INTERVAL_SECONDS.key(), () -> "45");
        registry.add(BuiltinConfigs.PLUGINS_SKIP_SYNC.key(), () -> "true");
        registry.add("audit.probe.interval-ms", () -> "60000");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthTokenRepository tokenRepository;

    @Autowired
    private DataRepository repository;

    @BeforeEach
    void resetSpool() throws java.io.IOException {
        java.nio.file.Files.createDirectories(spoolDirectory);
        java.nio.file.Files.writeString(spoolDirectory.resolve("pending.ndjson"), "");
    }

    @Test
    void getAuditStatusReturnsEnrichedFieldsWhenEnabled() throws Exception {
        String token = adminBearerToken();

        this.mockMvc.perform(
                        get("/api/audit/status")
                                .header("Authorization", "Bearer " + token)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.auditEnabled").value(true))
                .andExpect(jsonPath("$.data.auditDisabledReason").isEmpty())
                .andExpect(jsonPath("$.data.primaryWritable").value(true))
                .andExpect(jsonPath("$.data.pendingSpoolCount").value(0))
                .andExpect(jsonPath("$.data.archiveEnabled").value(true))
                .andExpect(jsonPath("$.data.drainIntervalSeconds").value(45))
                .andExpect(jsonPath("$.data.lastProbeAt").exists());
    }

    private String adminBearerToken() {
        com.openrecordsmanager.user.User admin = this.repository.userRepo.findByUsername("admin").orElseThrow();
        AuthToken token = new AuthToken(AuthService.generateToken(), admin, Instant.now().plusSeconds(3600));
        this.tokenRepository.saveAndFlush(token);
        return token.getToken();
    }
}
