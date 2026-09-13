package com.openrecordsmanager.config;

import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.template.property.PropertyType;
import com.openrecordsmanager.audit.AuditPolicyService;
import com.openrecordsmanager.audit.AuditService;
import com.openrecordsmanager.audit.persistence.AuditEventRepository;
import com.openrecordsmanager.audit.persistence.AuditPolicyRepository;
import com.openrecordsmanager.auth.AuthProviderRepository;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.filestore.middleware.MiddlewareRepository;
import com.openrecordsmanager.filestore.store.FileStoreRepository;
import com.openrecordsmanager.list.ListElementRepository;
import com.openrecordsmanager.list.ListTypeRepository;
import com.openrecordsmanager.plugin.PluginRepository;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.property.ObjectPropertyRepository;
import com.openrecordsmanager.record.RecordRepository;
import com.openrecordsmanager.recordtype.RecordTypeRepository;
import com.openrecordsmanager.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigServiceGetValueTest {

    @Mock
    private ConfigRepository configRepository;

    @Mock
    private ComponentCatalog catalog;

    @Mock
    private AuditService auditService;

    @Mock
    private AuditPolicyService auditPolicyService;

    private StandardEnvironment environment;
    private JdbcTemplate databaseConfigJdbc;
    private AtomicInteger databaseConfigQueries;
    private ConfigService configService;

    @BeforeEach
    void setUp() {
        this.environment = new StandardEnvironment();
        this.databaseConfigQueries = new AtomicInteger();
        this.databaseConfigJdbc = mock(JdbcTemplate.class);
        lenient().when(this.databaseConfigJdbc.query(anyString(), any(RowMapper.class), any()))
                .thenAnswer(invocation -> {
                    this.databaseConfigQueries.incrementAndGet();
                    throw new IllegalStateException("DatabaseConfigSource must not be queried via ConfigService");
                });
        this.environment.getPropertySources().addLast(new DatabaseConfigSource(this.databaseConfigJdbc));
        this.environment.getPropertySources().addFirst(new MapPropertySource(
                "configurationProperties",
                Map.of(BuiltinConfigs.AUDIT_ENABLED.key(), "should-be-ignored")
        ));

        DataRepository dataRepository = new DataRepository(
                mock(AuditEventRepository.class),
                mock(AuditPolicyRepository.class),
                mock(AuthProviderRepository.class),
                this.configRepository,
                mock(ListElementRepository.class),
                mock(ListTypeRepository.class),
                mock(ObjectPropertyRepository.class),
                mock(RecordTypeRepository.class),
                mock(RecordRepository.class),
                mock(FileStoreRepository.class),
                mock(MiddlewareRepository.class),
                mock(PluginRepository.class),
                mock(UserRepository.class)
        );

        this.configService = new ConfigService(
                this.environment,
                dataRepository,
                this.catalog,
                this.auditService,
                this.auditPolicyService
        );
    }

    @Test
    void getValueReadsDatabaseRowViaRepositoryNotDatabaseConfigSource() {
        ConfigItem stored = mock(ConfigItem.class);
        when(stored.getValue()).thenReturn(PropertyType.toTree(false));
        when(this.configRepository.findByConfigKey(BuiltinConfigs.AUDIT_ENABLED.key()))
                .thenReturn(Optional.of(stored));

        assertFalse(this.configService.getValue(BuiltinConfigs.AUDIT_ENABLED));
        assertEquals(0, this.databaseConfigQueries.get());
        verify(this.configRepository).findByConfigKey(BuiltinConfigs.AUDIT_ENABLED.key());
        verify(this.databaseConfigJdbc, never()).query(anyString(), any(RowMapper.class), any());
    }

    @Test
    void getValuePrefersNonDatabaseEnvironmentOverride() {
        this.environment.getPropertySources().addFirst(new MapPropertySource(
                "testOverrides",
                Map.of(BuiltinConfigs.AUDIT_ENABLED.key(), "false")
        ));

        assertFalse(this.configService.getValue(BuiltinConfigs.AUDIT_ENABLED));
        assertEquals(0, this.databaseConfigQueries.get());
        verify(this.configRepository, never()).findByConfigKey(anyString());
    }

    @Test
    void getValueFallsBackToDefaultWhenUnset() {
        when(this.configRepository.findByConfigKey(BuiltinConfigs.AUDIT_ENABLED.key()))
                .thenReturn(Optional.empty());

        assertTrue(this.configService.getValue(BuiltinConfigs.AUDIT_ENABLED));
        assertEquals(0, this.databaseConfigQueries.get());
    }
}
