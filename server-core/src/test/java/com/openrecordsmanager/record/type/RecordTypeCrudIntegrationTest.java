package com.openrecordsmanager.record.type;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.builtin.BuiltinPlugin;
import com.openrecordsmanager.api.builtin.BuiltinPropertyIds;
import com.openrecordsmanager.api.template.recordtype.SecurityFilterUsage;
import com.openrecordsmanager.audit.AuditTestSupport;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.database.SqliteTestSupport;
import com.openrecordsmanager.location.user.User;
import com.openrecordsmanager.plugin.exception.BuiltinResourceImmutableException;
import com.openrecordsmanager.property.dto.TypePropertyAssignment;
import com.openrecordsmanager.record.type.dto.NewRecordTypeRequest;
import com.openrecordsmanager.record.type.dto.RecordTypeResponse;
import com.openrecordsmanager.record.type.dto.UpdateRecordTypeRequest;
import com.openrecordsmanager.rest.exception.ResourceAlreadyExistsException;
import com.openrecordsmanager.rest.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.node.JsonNodeFactory;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RecordTypeCrudIntegrationTest {

    private static final ResourceIdentifier TITLE =
            new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinPropertyIds.TITLE);
    private static final ResourceIdentifier NOTES =
            new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinPropertyIds.NOTES);
    private static final ResourceIdentifier TYPE_ID = ResourceIdentifier.valueOf("test:admin_record_type");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SqliteTestSupport.registerPrimaryMemoryDatabase(registry, RecordTypeCrudIntegrationTest.class);
        registry.add(BuiltinConfigs.PLUGINS_SKIP_SYNC.key(), () -> "true");
        registry.add(BuiltinConfigs.COOKIE_SECURE.key(), () -> "false");
    }

    @Autowired
    private RecordTypeService recordTypeService;

    @Autowired
    private DataRepository repository;

    private User admin;

    @BeforeEach
    void setUp() {
        this.admin = this.repository.userRepo.findByName("admin").orElseThrow();
    }

    @Test
    void createUpdateAndRejectInvalidCases() {
        RecordTypeResponse created = AuditTestSupport.withAudit(this.admin, () -> this.recordTypeService.create(
                new NewRecordTypeRequest(
                        TYPE_ID,
                        Set.of("text/plain", "application/pdf"),
                        null,
                        SecurityFilterUsage.SHOW_ALL,
                        List.of(new TypePropertyAssignment(TITLE, null))
                )
        ));

        assertEquals(TYPE_ID, created.id());
        assertEquals(SecurityFilterUsage.SHOW_ALL, created.securityFilterUsage());
        assertEquals(Set.of("text/plain", "application/pdf"), created.contentTypes());
        assertEquals(1, created.properties().size());
        assertTrue(created.properties().stream().anyMatch(p -> p.property().id().equals(TITLE)));

        RecordTypeResponse updated = AuditTestSupport.withAudit(this.admin, () -> this.recordTypeService.update(
                TYPE_ID,
                new UpdateRecordTypeRequest(
                        Set.of("image/png"),
                        "actor.isAdmin()",
                        SecurityFilterUsage.HIDE_FILES,
                        List.of(
                                new TypePropertyAssignment(TITLE, null),
                                new TypePropertyAssignment(NOTES, JsonNodeFactory.instance.stringNode("note"))
                        )
                )
        ));

        assertEquals(Set.of("image/png"), updated.contentTypes());
        assertEquals("actor.isAdmin()", updated.securityFilter());
        assertEquals(SecurityFilterUsage.HIDE_FILES, updated.securityFilterUsage());
        assertEquals(2, updated.properties().size());
        assertTrue(updated.properties().stream().anyMatch(
                p -> p.property().id().equals(NOTES) && p.defaultValue() != null && p.defaultValue().asString().equals("note")
        ));

        assertThrows(ResourceAlreadyExistsException.class, () -> AuditTestSupport.withAudit(this.admin, () ->
                this.recordTypeService.create(new NewRecordTypeRequest(
                        TYPE_ID,
                        Set.of(),
                        null,
                        SecurityFilterUsage.SHOW_ALL,
                        List.of()
                ))
        ));

        ResourceIdentifier builtinId = new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, "not_a_real_type");
        assertThrows(BuiltinResourceImmutableException.class, () -> AuditTestSupport.withAudit(this.admin, () ->
                this.recordTypeService.create(new NewRecordTypeRequest(
                        builtinId,
                        Set.of(),
                        null,
                        SecurityFilterUsage.SHOW_ALL,
                        List.of()
                ))
        ));

        ResourceIdentifier unknownProperty = ResourceIdentifier.valueOf("test:missing_property");
        assertThrows(ResourceNotFoundException.class, () -> AuditTestSupport.withAudit(this.admin, () ->
                this.recordTypeService.create(new NewRecordTypeRequest(
                        ResourceIdentifier.valueOf("test:admin_record_type_bad_prop"),
                        Set.of(),
                        null,
                        SecurityFilterUsage.SHOW_ALL,
                        List.of(new TypePropertyAssignment(unknownProperty, null))
                ))
        ));
    }
}
