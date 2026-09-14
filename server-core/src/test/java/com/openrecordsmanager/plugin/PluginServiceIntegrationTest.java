package com.openrecordsmanager.plugin;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.filestore.FileStoreType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.audit.persistence.AuditEventEntity;
import com.openrecordsmanager.audit.persistence.AuditPolicyEntity;
import com.openrecordsmanager.audit.persistence.AuditPolicyId;
import com.openrecordsmanager.auth.TestAuthTokens;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.filestore.store.FileStore;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PluginServiceIntegrationTest {

    private static final Path PLUGINS_DIR = Path.of("build/test-plugin-service-" + UUID.randomUUID());
    private static final Path FILE_STORE_ROOT = Path.of("build/test-plugin-filestore-" + UUID.randomUUID());
    private static final AtomicReference<String> DEFAULT_FILE_STORE = new AtomicReference<>("");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws IOException {
        Files.createDirectories(PLUGINS_DIR);
        Files.createDirectories(FILE_STORE_ROOT);
        restorePluginJars();
        com.openrecordsmanager.database.SqliteTestSupport.registerPrimaryMemoryDatabase(registry, PluginServiceIntegrationTest.class);
        registry.add(BuiltinConfigs.PLUGINS_DIRECTORY.key(), PLUGINS_DIR::toString);
        registry.add(BuiltinConfigs.PLUGINS_SKIP_SYNC.key(), () -> "true");
        registry.add(BuiltinConfigs.PLUGINS_SYNC_INTERVAL_MS_KEY, () -> "600000");
        registry.add(BuiltinConfigs.COOKIE_SECURE.key(), () -> "false");
        registry.add(BuiltinConfigs.DEFAULT_FILE_STORE.key(), DEFAULT_FILE_STORE::get);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataRepository repository;

    @Autowired
    private TestAuthTokens testAuthTokens;

    @Autowired
    private ComponentCatalog catalog;

    @Autowired
    private PluginSyncService pluginSyncService;

    @Autowired
    private PluginManager pluginManager;

    @BeforeEach
    void setUp() throws IOException {
        restorePluginJars();
        this.pluginManager.reload(null);
        this.catalog.reload(this.pluginManager);

        if (DEFAULT_FILE_STORE.get().isEmpty()) {
            FileStoreType<?> localType = this.catalog.getRegistry(ComponentTypes.FILE_STORE)
                    .get(ResourceIdentifier.valueOf("filestore_local:local"))
                    .orElseThrow();
            FileStore store = new FileStore(this.catalog, localType, Map.of("rootDir", FILE_STORE_ROOT.toString()));
            this.repository.fileStoreRepo.saveAndFlush(store);
            DEFAULT_FILE_STORE.set(store.getId().toString());
        }

        this.repository.auditPolicyRepo.saveAndFlush(new AuditPolicyEntity(
                new AuditPolicyId(AuditEntityType.PLUGIN, AuditOperation.CREATE),
                true,
                false,
                "Plugin created",
                "test"
        ));
        this.repository.auditPolicyRepo.saveAndFlush(new AuditPolicyEntity(
                new AuditPolicyId(AuditEntityType.PLUGIN, AuditOperation.UPDATE),
                true,
                false,
                "Plugin updated",
                "test"
        ));
        this.repository.auditPolicyRepo.saveAndFlush(new AuditPolicyEntity(
                new AuditPolicyId(AuditEntityType.PLUGIN, AuditOperation.DELETE),
                true,
                false,
                "Plugin deleted",
                "test"
        ));

        this.repository.pluginRepo.deleteAll();
    }

    private static void restorePluginJars() throws IOException {
        Files.createDirectories(PLUGINS_DIR);
        try (var existing = Files.list(PLUGINS_DIR)) {
            existing.filter(path -> path.getFileName().toString().endsWith(".jar")).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        Path sourceDir = Path.of("plugins");
        try (var jars = Files.list(sourceDir)) {
            jars.filter(path -> {
                        String name = path.getFileName().toString();
                        return name.endsWith(".jar") && !name.startsWith("upload-");
                    })
                    .forEach(source -> {
                        try {
                            Files.copy(source, PLUGINS_DIR.resolve(source.getFileName()), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }

    private String adminBearerToken() {
        return this.testAuthTokens.adminAccessToken();
    }

    @Test
    void uploadCreatesPluginAndAuditMetadataContainsFileHash() throws Exception {
        byte[] jarBytes = Files.readAllBytes(PLUGINS_DIR.resolve("filestore-local-0.1.0.jar"));
        String token = this.adminBearerToken();

        this.mockMvc.perform(
                        multipart("/api/plugins")
                                .file(new MockMultipartFile("file", "filestore-local-0.1.0.jar", "application/java-archive", jarBytes))
                                .param("type", "JAR")
                                .header("Authorization", "Bearer " + token)
                                .header("X-ORM-Audit-Comment", "install plugin")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("filestore_local"))
                .andExpect(jsonPath("$.data.displayName").value("Local File Store"))
                .andExpect(jsonPath("$.data.description").value(
                        "Store files on the local filesystem under a configured root directory."
                ))
                .andExpect(jsonPath("$.data.version").value("0.1.0"))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.loaded").value(true));

        assertTrue(this.repository.pluginRepo.findById("filestore_local").isPresent());

        AuditEventEntity event = this.repository.auditEventRepo
                .findByTargetTypeAndTargetIdOrderByOccurredAtDesc(
                        AuditEntityType.PLUGIN.key(),
                        "filestore_local",
                        Pageable.ofSize(10)
                )
                .stream()
                .filter(auditEvent -> auditEvent.operation == AuditOperation.CREATE)
                .findFirst()
                .orElseThrow();

        assertNotNull(event.metadata);
        assertTrue(event.metadata.contains("\"fileHash\""));
        assertTrue(event.metadata.contains("\"hashAlgorithm\":\"SHA-256\""));
        assertTrue(event.metadata.contains("\"version\":\"0.1.0\""));
    }

    @Test
    void disablePluginExcludesItFromLoadedSet() throws Exception {
        this.pluginSyncService.syncAndReload(true);

        String token = this.adminBearerToken();

        this.mockMvc.perform(
                        put("/api/plugins/filestore_local")
                                .header("Authorization", "Bearer " + token)
                                .header("X-ORM-Audit-Comment", "disable plugin")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "enabled": false
                                        }
                                        """)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.loaded").value(false));

        assertFalse(this.repository.pluginRepo.findById("filestore_local").orElseThrow().isEnabled());
    }

    @Test
    void deleteRemovesPluginFromDatabase() throws Exception {
        this.pluginSyncService.syncAndReload(true);
        assertTrue(this.repository.pluginRepo.existsById("filestore_s3"));

        String token = this.adminBearerToken();

        this.mockMvc.perform(
                        delete("/api/plugins/filestore_s3")
                                .header("Authorization", "Bearer " + token)
                                .header("X-ORM-Audit-Comment", "remove plugin")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());

        assertFalse(this.repository.pluginRepo.existsById("filestore_s3"));
    }
}
