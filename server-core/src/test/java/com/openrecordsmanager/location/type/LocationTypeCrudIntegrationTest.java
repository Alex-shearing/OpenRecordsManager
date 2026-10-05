package com.openrecordsmanager.location.type;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.builtin.BuiltinLocationTypeIds;
import com.openrecordsmanager.api.builtin.BuiltinPlugin;
import com.openrecordsmanager.api.builtin.BuiltinPropertyIds;
import com.openrecordsmanager.api.location.LocationKind;
import com.openrecordsmanager.audit.AuditTestSupport;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.database.SqliteTestSupport;
import com.openrecordsmanager.location.type.dto.LocationTypeResponse;
import com.openrecordsmanager.location.type.dto.NewLocationTypeRequest;
import com.openrecordsmanager.location.type.dto.UpdateLocationTypeRequest;
import com.openrecordsmanager.location.user.User;
import com.openrecordsmanager.plugin.exception.BuiltinResourceImmutableException;
import com.openrecordsmanager.property.dto.TypePropertyAssignment;
import com.openrecordsmanager.rest.exception.ResourceAlreadyExistsException;
import com.openrecordsmanager.rest.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LocationTypeCrudIntegrationTest {

    private static final ResourceIdentifier NAME =
            new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinPropertyIds.NAME);
    private static final ResourceIdentifier NOTES =
            new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinPropertyIds.NOTES);
    private static final ResourceIdentifier TYPE_ID = ResourceIdentifier.valueOf("test:admin_location_type");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SqliteTestSupport.registerPrimaryMemoryDatabase(registry, LocationTypeCrudIntegrationTest.class);
        registry.add(BuiltinConfigs.PLUGINS_SKIP_SYNC.key(), () -> "true");
        registry.add(BuiltinConfigs.COOKIE_SECURE.key(), () -> "false");
    }

    @Autowired
    private LocationTypeService locationTypeService;

    @Autowired
    private DataRepository repository;

    private User admin;

    @BeforeEach
    void setUp() {
        this.admin = this.repository.userRepo.findByName("admin").orElseThrow();
    }

    @Test
    void createUpdateAndRejectInvalidCases() {
        LocationTypeResponse created = AuditTestSupport.withAudit(this.admin, () -> this.locationTypeService.create(
                new NewLocationTypeRequest(
                        TYPE_ID,
                        LocationKind.USER,
                        List.of(new TypePropertyAssignment(NAME, null))
                )
        ));

        assertEquals(TYPE_ID, created.id());
        assertEquals(LocationKind.USER, created.kind());
        assertEquals(1, created.properties().size());
        assertTrue(created.properties().stream().anyMatch(p -> p.property().id().equals(NAME)));

        LocationTypeResponse updated = AuditTestSupport.withAudit(this.admin, () -> this.locationTypeService.update(
                TYPE_ID,
                new UpdateLocationTypeRequest(
                        LocationKind.GROUP,
                        List.of(
                                new TypePropertyAssignment(NAME, null),
                                new TypePropertyAssignment(NOTES, null)
                        )
                )
        ));

        assertEquals(LocationKind.GROUP, updated.kind());
        assertEquals(2, updated.properties().size());

        assertThrows(ResourceAlreadyExistsException.class, () -> AuditTestSupport.withAudit(this.admin, () ->
                this.locationTypeService.create(new NewLocationTypeRequest(
                        TYPE_ID,
                        LocationKind.USER,
                        List.of()
                ))
        ));

        ResourceIdentifier builtinId = new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinLocationTypeIds.USER);
        assertThrows(BuiltinResourceImmutableException.class, () -> AuditTestSupport.withAudit(this.admin, () ->
                this.locationTypeService.update(
                        builtinId,
                        new UpdateLocationTypeRequest(LocationKind.GROUP, List.of())
                )
        ));

        ResourceIdentifier unknownProperty = ResourceIdentifier.valueOf("test:missing_location_property");
        assertThrows(ResourceNotFoundException.class, () -> AuditTestSupport.withAudit(this.admin, () ->
                this.locationTypeService.create(new NewLocationTypeRequest(
                        ResourceIdentifier.valueOf("test:admin_location_type_bad_prop"),
                        LocationKind.USER,
                        List.of(new TypePropertyAssignment(unknownProperty, null))
                ))
        ));
    }
}
