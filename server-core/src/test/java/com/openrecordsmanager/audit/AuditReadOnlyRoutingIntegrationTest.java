package com.openrecordsmanager.audit;

import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.audit.persistence.AuditEventRepository;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuditReadOnlyRoutingIntegrationTest {

    private static final Path BASE = Path.of("build/audit-routing-" + UUID.randomUUID());
    private static final Path DB_FILE = BASE.resolve("orm_routing.db");
    private static final Path SPOOL = BASE.resolve("spool");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) throws Exception {
        Files.createDirectories(SPOOL);
        String writeUrl = "jdbc:sqlite:" + DB_FILE.toAbsolutePath();
        String readUrl = writeUrl + "?open_mode=1";
        registry.add("server.database.primary.url", () -> writeUrl);
        registry.add("server.database.primary.driver-class-name", () -> "org.sqlite.JDBC");
        registry.add("server.database.read-only.url", () -> readUrl);
        registry.add("server.database.read-only.driver-class-name", () -> "org.sqlite.JDBC");
        registry.add("server.plugins.skip_startup_check", () -> "true");
        registry.add("audit.spool.directory", () -> SPOOL.toString());
        registry.add("audit.drain.fixed-delay-ms", () -> "60000");
        registry.add("audit.probe.interval-ms", () -> "60000");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration/sqlite");
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AuditService auditService;
    @Autowired
    private DatabaseWritableProbe probe;
    @Autowired
    private AuditEventRepository auditEventRepo;
    @Autowired
    private Environment env;
    @Autowired
    @Qualifier("writeDataSource")
    private DataSource writeDataSource;
    @Autowired
    @Qualifier("readDataSource")
    private DataSource readDataSource;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private PlatformTransactionManager txManager;
    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @BeforeEach
    void ready() {
        this.probe.markWriteSucceeded();
        AuditContext.begin(null, "tester", null, true);
    }

    @Test
    void datasourceUrlsAreDistinctAndWriteIsWritable() throws Exception {
        String primary = this.env.getProperty("server.database.primary.url");
        String read = this.env.getProperty("server.database.read-only.url");
        assertNotNull(primary);
        assertNotNull(read);
        assertFalse(primary.contains("open_mode=1"), primary);
        assertTrue(read.contains("open_mode=1"), read);
        assertInstanceOf(LazyConnectionDataSourceProxy.class, this.dataSource);

        try (Connection write = this.writeDataSource.getConnection();
             Connection readCon = this.readDataSource.getConnection()) {
            assertFalse(write.isReadOnly(), "write DS should not be read-only");
            assertTrue(readCon.isReadOnly(), "read DS should be read-only");
        }
    }

    @Test
    void connectionHandlingReleasesAfterTransactionForRouting() {
        SessionFactoryImplementor sfi = this.entityManager
                .unwrap(Session.class)
                .getSessionFactory()
                .unwrap(SessionFactoryImplementor.class);
        assertEquals(
                PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION,
                sfi.getSessionFactoryOptions().getPhysicalConnectionHandlingMode()
        );
    }

    @Test
    void auditPersistAfterReadOnlyTransactionUsesWriteConnection() {
        long before = this.auditEventRepo.count();
        TransactionTemplate readTx = new TransactionTemplate(this.txManager);
        readTx.setReadOnly(true);

        readTx.executeWithoutResult(status -> {
            assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
            this.auditService.addEvent(AuditOperation.READ, AuditEntityType.AUTH_PROVIDER, "*");
        });

        assertTrue(this.auditEventRepo.count() > before, "expected audit rows after read-only tx commit");
        assertTrue(this.probe.isWritable(), "probe should stay writable");
    }

    @Test
    void providersEndpointDoesNotFailAuditWithReadonly() throws Exception {
        this.mockMvc.perform(get("/api/auth/providers").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        assertTrue(this.probe.isWritable(), "probe should remain writable after providers list audit");
    }
}
