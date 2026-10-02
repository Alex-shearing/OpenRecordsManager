package com.openrecordsmanager.plugin;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.filestore.FileStoreType;
import com.openrecordsmanager.api.types.ComponentTypes;
import com.openrecordsmanager.audit.AuditTestSupport;
import com.openrecordsmanager.audit.persistence.AuditEventEntity;
import com.openrecordsmanager.audit.persistence.AuditPolicyEntity;
import com.openrecordsmanager.audit.persistence.AuditPolicyId;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.filestore.store.FileStore;
import com.openrecordsmanager.plugin.dto.PluginResponse;
import com.openrecordsmanager.plugin.dto.PluginTypeRequest;
import com.openrecordsmanager.plugin.dto.UpdatePluginRequest;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.location.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
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
    private DataRepository repository;

    @Autowired
    private ComponentCatalog catalog;

    @Autowired
    private PluginSyncService pluginSyncService;

    @Autowired
    private PluginManager pluginManager;

    @Autowired
    private PluginService pluginService;

    private User admin;

    @BeforeEach
    void setUp() throws IOException {
        this.admin = this.repository.userRepo.findByUsername("admin").orElseThrow();
        restorePluginJars();
        this.repository.pluginRepo.deleteAll();
        this.pluginManager.reload(this.catalog);

        if (DEFAULT_FILE_STORE.get().isEmpty()) {
            FileStoreType<?> localType = this.catalog.getRegistry(ComponentTypes.FILE_STORE)
                    .get(ResourceIdentifier.valueOf("filestore_local:local"))
                    .orElseThrow();
            FileStore store = new FileStore(this.catalog, "Test local store", localType, Map.of("rootDir", FILE_STORE_ROOT.toString()));
            this.repository.fileStoreRepo.saveAndFlush(store);
            DEFAULT_FILE_STORE.set(store.getId().toString());
        }

        this.repository.auditPolicyRepo.saveAndFlush(new AuditPolicyEntity(new AuditPolicyId(AuditEntityType.PLUGIN, AuditOperation.CREATE), true, false));
        this.repository.auditPolicyRepo.saveAndFlush(new AuditPolicyEntity(new AuditPolicyId(AuditEntityType.PLUGIN, AuditOperation.UPDATE), true, false));
        this.repository.auditPolicyRepo.saveAndFlush(new AuditPolicyEntity(new AuditPolicyId(AuditEntityType.PLUGIN, AuditOperation.DELETE), true, false));

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

    @Test
    void getAllIncludesDiscoveredPluginsWhenDatabaseEmpty() {
        assertTrue(this.repository.pluginRepo.findAll().isEmpty());
        assertTrue(this.pluginManager.isLoaded("builtin"));

        var plugins = this.pluginService.getAll(true);

        assertTrue(
                plugins.stream().anyMatch(plugin -> "filestore_local".equals(plugin.id()) && plugin.loaded()),
                "expected discovered local plugin in list"
        );
        assertTrue(
                plugins.stream().anyMatch(plugin -> "filestore_s3".equals(plugin.id()) && plugin.loaded()),
                "expected discovered s3 plugin in list"
        );
    }

    @Test
    void uploadCreatesPluginAndAuditMetadataContainsFileHash() throws Exception {
        byte[] jarBytes = Files.readAllBytes(PLUGINS_DIR.resolve("filestore-local-0.1.0.jar"));

        PluginResponse uploaded = AuditTestSupport.withAudit(this.admin, () -> this.pluginService.upload(
                new ByteArrayInputStream(jarBytes),
                PluginTypeRequest.JAR
        ));

        assertEquals("filestore_local", uploaded.id());
        assertEquals("0.1.0", uploaded.version());
        assertTrue(uploaded.enabled());
        assertTrue(uploaded.loaded());
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
    void disablePluginExcludesItFromLoadedSet() {
        this.pluginSyncService.syncAndReload(true);

        PluginResponse updated = AuditTestSupport.withAudit(this.admin, () -> this.pluginService.update(
                "filestore_local",
                new UpdatePluginRequest(false)
        ));

        assertFalse(updated.enabled());
        assertFalse(updated.loaded());
        assertFalse(this.repository.pluginRepo.findById("filestore_local").orElseThrow().isEnabled());
    }

    @Test
    void deleteRemovesPluginFromDatabase() throws Exception {
        this.pluginSyncService.syncAndReload(true);
        assertTrue(this.repository.pluginRepo.existsById("filestore_s3"));

        AuditTestSupport.withAudit(this.admin, () -> {
            this.pluginService.delete("filestore_s3");
            return null;
        });

        assertFalse(this.repository.pluginRepo.existsById("filestore_s3"));
    }

}
