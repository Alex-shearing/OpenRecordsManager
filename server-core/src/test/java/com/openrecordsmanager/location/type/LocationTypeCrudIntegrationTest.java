package com.openrecordsmanager.location.type;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.builtin.BuiltinLocationTypeIds;
import com.openrecordsmanager.api.builtin.BuiltinPlugin;
import com.openrecordsmanager.api.builtin.BuiltinPropertyIds;
import com.openrecordsmanager.api.errors.ApiException;
import com.openrecordsmanager.api.property.ManagedObjectKind;
import com.openrecordsmanager.audit.AuditTestSupport;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.database.SqliteTestSupport;
import com.openrecordsmanager.location.LocationService;
import com.openrecordsmanager.location.dto.NewLocationRequest;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LocationTypeCrudIntegrationTest {

    private static final ResourceIdentifier NAME =
            new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinPropertyIds.NAME);
    private static final ResourceIdentifier NOTES =
            new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinPropertyIds.NOTES);
    private static final ResourceIdentifier TYPE_ID = ResourceIdentifier.valueOf("test:admin_location_type");
    private static final ResourceIdentifier LOCATION_ROOT =
            new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinLocationTypeIds.LOCATION);
    private static final ResourceIdentifier ABSTRACT_TYPE_ID =
            ResourceIdentifier.valueOf("test:abstract_location_type");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SqliteTestSupport.registerPrimaryMemoryDatabase(registry, LocationTypeCrudIntegrationTest.class);
        registry.add(BuiltinConfigs.PLUGINS_SKIP_SYNC.key(), () -> "true");
        registry.add(BuiltinConfigs.COOKIE_SECURE.key(), () -> "false");
    }

    @Autowired
    private LocationTypeService locationTypeService;

    @Autowired
    private LocationService locationService;

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
                        ManagedObjectKind.USER,
                        false,
                        LOCATION_ROOT,
                        List.of(new TypePropertyAssignment(NAME, null))
                )
        ));

        assertEquals(TYPE_ID, created.id());
        assertEquals(ManagedObjectKind.USER, created.kind());
        assertFalse(created.abstractType());
        assertEquals(LOCATION_ROOT, created.baseTypeId());
        assertEquals(1, created.declaredProperties().size());
        assertTrue(created.declaredProperties().stream().anyMatch(p -> p.property().id().equals(NAME)));
        assertTrue(created.effectiveProperties().stream().anyMatch(p -> p.property().id().equals(NAME)));
        assertTrue(
                created.effectiveProperties().size() >= created.declaredProperties().size(),
                "effective properties include inherited bindings from the base type"
        );

        LocationTypeResponse updated = AuditTestSupport.withAudit(this.admin, () -> this.locationTypeService.update(
                TYPE_ID,
                new UpdateLocationTypeRequest(
                        ManagedObjectKind.GROUP,
                        false,
                        LOCATION_ROOT,
                        List.of(
                                new TypePropertyAssignment(NAME, null),
                                new TypePropertyAssignment(NOTES, null)
                        )
                )
        ));

        assertEquals(ManagedObjectKind.GROUP, updated.kind());
        assertEquals(2, updated.declaredProperties().size());
        assertTrue(updated.effectiveProperties().size() >= updated.declaredProperties().size());

        assertThrows(ResourceAlreadyExistsException.class, () -> AuditTestSupport.withAudit(this.admin, () ->
                this.locationTypeService.create(new NewLocationTypeRequest(
                        TYPE_ID,
                        ManagedObjectKind.USER,
                        false,
                        null,
                        List.of()
                ))
        ));

        ResourceIdentifier builtinId = new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinLocationTypeIds.USER);
        assertThrows(BuiltinResourceImmutableException.class, () -> AuditTestSupport.withAudit(this.admin, () ->
                this.locationTypeService.update(
                        builtinId,
                        new UpdateLocationTypeRequest(ManagedObjectKind.GROUP, false, null, List.of())
                )
        ));

        ResourceIdentifier unknownProperty = ResourceIdentifier.valueOf("test:missing_location_property");
        assertThrows(ResourceNotFoundException.class, () -> AuditTestSupport.withAudit(this.admin, () ->
                this.locationTypeService.create(new NewLocationTypeRequest(
                        ResourceIdentifier.valueOf("test:admin_location_type_bad_prop"),
                        ManagedObjectKind.USER,
                        false,
                        null,
                        List.of(new TypePropertyAssignment(unknownProperty, null))
                ))
        ));
    }

    @Test
    void abstractTypesCannotBeInstantiatedAndAnyRequiresAbstract() {
        assertThrows(IllegalArgumentException.class, () -> AuditTestSupport.withAudit(this.admin, () ->
                this.locationTypeService.create(new NewLocationTypeRequest(
                        ResourceIdentifier.valueOf("test:any_without_abstract"),
                        null,
                        false,
                        null,
                        List.of()
                ))
        ));

        LocationTypeResponse abstractType = AuditTestSupport.withAudit(this.admin, () -> this.locationTypeService.create(
                new NewLocationTypeRequest(
                        ABSTRACT_TYPE_ID,
                        null,
                        true,
                        null,
                        List.of()
                )
        ));
        assertTrue(abstractType.abstractType());
        assertNull(abstractType.kind());

        assertThrows(ApiException.class, () -> AuditTestSupport.withAudit(this.admin, () ->
                this.locationService.create(new NewLocationRequest(ABSTRACT_TYPE_ID, null, "nope", Map.of()))
        ));
    }
}
