package com.openrecordsmanager.location.relationship;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.builtin.BuiltinLocationTypeIds;
import com.openrecordsmanager.api.builtin.BuiltinPlugin;
import com.openrecordsmanager.api.builtin.BuiltinRelationshipTypeIds;
import com.openrecordsmanager.api.errors.ApiException;
import com.openrecordsmanager.api.location.LocationKind;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.database.SqliteTestSupport;
import com.openrecordsmanager.location.LocationService;
import com.openrecordsmanager.location.dto.LocationResponse;
import com.openrecordsmanager.location.dto.NewLocationRequest;
import com.openrecordsmanager.location.dto.UpdateLocationRequest;
import com.openrecordsmanager.location.relationship.dto.LocationRelationshipResponse;
import com.openrecordsmanager.location.relationship.dto.LocationRelationshipTypeResponse;
import com.openrecordsmanager.location.relationship.dto.NewLocationRelationshipRequest;
import com.openrecordsmanager.location.relationship.dto.NewLocationRelationshipTypeRequest;
import com.openrecordsmanager.location.relationship.dto.UpdateLocationRelationshipTypeRequest;
import com.openrecordsmanager.location.user.User;
import com.openrecordsmanager.plugin.exception.BuiltinResourceImmutableException;
import com.openrecordsmanager.rest.exception.ResourceAlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LocationRelationshipIntegrationTest {

    private static final ResourceIdentifier USER_TYPE =
            new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinLocationTypeIds.USER);
    private static final ResourceIdentifier GROUP_TYPE =
            new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinLocationTypeIds.GROUP);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SqliteTestSupport.registerPrimaryMemoryDatabase(registry, LocationRelationshipIntegrationTest.class);
        registry.add(BuiltinConfigs.PLUGINS_SKIP_SYNC.key(), () -> "true");
        registry.add(BuiltinConfigs.COOKIE_SECURE.key(), () -> "false");
    }

    @Autowired
    private LocationService locationService;

    @Autowired
    private LocationRelationshipTypeService relationshipTypeService;

    @Autowired
    private DataRepository repository;

    @Test
    void memberOfAndReportsToSupportIncomingInverseQueries() {
        LocationResponse alice = this.locationService.create(new NewLocationRequest(USER_TYPE, null, "alice", Map.of()));
        LocationResponse bob = this.locationService.create(new NewLocationRequest(USER_TYPE, null, "bob", Map.of()));
        LocationResponse engineering = this.locationService.create(new NewLocationRequest(GROUP_TYPE, null, "Engineering", Map.of()));

        LocationRelationshipResponse membership = this.locationService.createRelationship(
                alice.id(),
                new NewLocationRelationshipRequest(
                        engineering.id(),
                        new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinRelationshipTypeIds.MEMBER_OF),
                        null
                )
        );
        assertEquals(
                new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinRelationshipTypeIds.MEMBER_OF),
                membership.typeId()
        );

        LocationRelationshipResponse reportsTo = this.locationService.createRelationship(
                alice.id(),
                new NewLocationRelationshipRequest(
                        bob.id(),
                        new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinRelationshipTypeIds.REPORTS_TO),
                        null
                )
        );

        List<LocationRelationshipResponse> aliceOutgoing = this.locationService.listRelationships(
                alice.id(),
                RelationshipDirection.OUTGOING,
                null
        );
        assertEquals(2, aliceOutgoing.size());

        List<LocationRelationshipResponse> engineeringMembers = this.locationService.listRelationships(
                engineering.id(),
                RelationshipDirection.INCOMING,
                new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinRelationshipTypeIds.MEMBER_OF)
        );
        assertEquals(1, engineeringMembers.size());
        assertEquals(alice.id(), engineeringMembers.getFirst().sourceId());

        List<LocationRelationshipResponse> bobReports = this.locationService.listRelationships(
                bob.id(),
                RelationshipDirection.INCOMING,
                new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinRelationshipTypeIds.REPORTS_TO)
        );
        assertEquals(1, bobReports.size());
        assertEquals(reportsTo.id(), bobReports.getFirst().id());

        // uniquePerSource: second active reports_to from alice must fail
        assertThrows(ApiException.class, () -> this.locationService.createRelationship(
                alice.id(),
                new NewLocationRelationshipRequest(
                        this.repository.userRepo.findByName("admin").orElseThrow().getId(),
                        new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinRelationshipTypeIds.REPORTS_TO),
                        null
                )
        ));

        // duplicate active edge
        assertThrows(ResourceAlreadyExistsException.class, () -> this.locationService.createRelationship(
                alice.id(),
                new NewLocationRelationshipRequest(
                        engineering.id(),
                        new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinRelationshipTypeIds.MEMBER_OF),
                        null
                )
        ));

        this.locationService.endRelationship(alice.id(), membership.id());
        assertTrue(this.locationService.listRelationships(
                alice.id(),
                RelationshipDirection.OUTGOING,
                new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinRelationshipTypeIds.MEMBER_OF)
        ).isEmpty());

        // ended edge can be recreated
        LocationRelationshipResponse remembership = this.locationService.createRelationship(
                alice.id(),
                new NewLocationRelationshipRequest(
                        engineering.id(),
                        new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinRelationshipTypeIds.MEMBER_OF),
                        null
                )
        );
        assertNotEquals(membership.id(), remembership.id());
    }

    @Test
    void customRelationshipTypesAndGroupCrud() {
        User admin = this.repository.userRepo.findByName("admin").orElseThrow();
        LocationResponse alice = this.locationService.create(new NewLocationRequest(USER_TYPE, null, "alice_sponsors", Map.of()));
        LocationResponse engineering = this.locationService.create(new NewLocationRequest(GROUP_TYPE, null, "Sponsors", Map.of()));
        LocationResponse renamed = this.locationService.update(
                admin,
                engineering.id(),
                new UpdateLocationRequest(null, null, "Sponsors Renamed", null)
        );
        assertEquals("Sponsors Renamed", renamed.name());
        assertEquals("Sponsors Renamed", this.locationService.get(engineering.id()).name());

        ResourceIdentifier sponsorsId = ResourceIdentifier.valueOf("test:sponsors");
        LocationRelationshipTypeResponse createdType = this.relationshipTypeService.create(
                new NewLocationRelationshipTypeRequest(
                        sponsorsId,
                        LocationKind.USER,
                        LocationKind.GROUP,
                        false
                )
        );
        assertEquals(sponsorsId, createdType.id());
        assertEquals(LocationKind.USER, createdType.sourceKind());
        assertEquals(LocationKind.GROUP, createdType.targetKind());
        assertFalse(createdType.uniquePerSource());

        assertTrue(this.relationshipTypeService.getAll().stream().anyMatch(t -> t.id().equals(sponsorsId)));
        assertEquals(sponsorsId, this.relationshipTypeService.get(sponsorsId).id());

        // kind mismatch: group cannot be source of USER->GROUP type
        ApiException kindMismatch = assertThrows(ApiException.class, () -> this.locationService.createRelationship(
                engineering.id(),
                new NewLocationRelationshipRequest(alice.id(), sponsorsId, null)
        ));
        assertEquals("validation_failed", kindMismatch.getError().code());
        assertTrue(kindMismatch.getFieldErrors().containsKey("typeId"));

        LocationRelationshipResponse sponsorship = this.locationService.createRelationship(
                alice.id(),
                new NewLocationRelationshipRequest(engineering.id(), sponsorsId, null)
        );
        assertEquals(sponsorsId, sponsorship.typeId());

        ResourceIdentifier newBuiltinId = new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, "not_a_real_type");
        assertThrows(BuiltinResourceImmutableException.class, () -> this.relationshipTypeService.create(
                new NewLocationRelationshipTypeRequest(newBuiltinId, LocationKind.ANY, LocationKind.ANY, true)
        ));

        ResourceIdentifier reportsTo = new ResourceIdentifier(
                BuiltinPlugin.BUILTIN_PLUGIN_NAME,
                BuiltinRelationshipTypeIds.REPORTS_TO
        );
        assertThrows(BuiltinResourceImmutableException.class, () -> this.relationshipTypeService.update(
                reportsTo,
                new UpdateLocationRelationshipTypeRequest(LocationKind.USER, null, null)
        ));
    }
}
