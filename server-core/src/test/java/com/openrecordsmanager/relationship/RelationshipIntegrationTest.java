package com.openrecordsmanager.relationship;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.builtin.BuiltinLocationTypeIds;
import com.openrecordsmanager.api.builtin.BuiltinPlugin;
import com.openrecordsmanager.api.builtin.BuiltinPropertyIds;
import com.openrecordsmanager.api.builtin.BuiltinRecordTypeIds;
import com.openrecordsmanager.api.builtin.BuiltinRelationshipTypeIds;
import com.openrecordsmanager.api.property.ManagedObjectKind;
import com.openrecordsmanager.api.errors.ApiException;
import com.openrecordsmanager.api.template.property.PropertyType;
import com.openrecordsmanager.api.template.recordtype.SecurityFilterUsage;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.database.SqliteTestSupport;
import com.openrecordsmanager.location.LocationService;
import com.openrecordsmanager.location.dto.LocationResponse;
import com.openrecordsmanager.location.dto.NewLocationRequest;
import com.openrecordsmanager.location.dto.UpdateLocationRequest;
import com.openrecordsmanager.location.type.LocationType;
import com.openrecordsmanager.location.user.User;
import com.openrecordsmanager.plugin.exception.BuiltinResourceImmutableException;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.record.RecordService;
import com.openrecordsmanager.record.dto.NewRecordRequest;
import com.openrecordsmanager.record.dto.RecordResponse;
import com.openrecordsmanager.record.type.RecordType;
import com.openrecordsmanager.template.ManagedObjectTypeProperty;
import com.openrecordsmanager.relationship.dto.NewRelationshipRequest;
import com.openrecordsmanager.relationship.dto.NewRelationshipTypeRequest;
import com.openrecordsmanager.relationship.dto.RelationshipResponse;
import com.openrecordsmanager.relationship.dto.RelationshipTypeResponse;
import com.openrecordsmanager.relationship.dto.UpdateRelationshipTypeRequest;
import com.openrecordsmanager.rest.exception.ResourceAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RelationshipIntegrationTest {

    private static final ResourceIdentifier LOCATION_TYPE =
            new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinLocationTypeIds.LOCATION);
    private static final ResourceIdentifier USER_TYPE =
            new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinLocationTypeIds.USER);
    private static final ResourceIdentifier GROUP_TYPE =
            new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinLocationTypeIds.GROUP);
    private static final ResourceIdentifier RECORD_ROOT =
            new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinRecordTypeIds.RECORD);
    private static final ResourceIdentifier TITLE =
            new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinPropertyIds.TITLE);
    private static final ResourceIdentifier RECORD_TYPE_A = ResourceIdentifier.valueOf("test:rel_record_type_a");
    private static final ResourceIdentifier RECORD_TYPE_B = ResourceIdentifier.valueOf("test:rel_record_type_b");
    private static final ResourceIdentifier OTHER_RECORD_TYPE = ResourceIdentifier.valueOf("test:loc_rec_rel_type_other");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SqliteTestSupport.registerPrimaryMemoryDatabase(registry, RelationshipIntegrationTest.class);
        registry.add(BuiltinConfigs.PLUGINS_SKIP_SYNC.key(), () -> "true");
        registry.add(BuiltinConfigs.COOKIE_SECURE.key(), () -> "false");
    }

    @Autowired
    private LocationService locationService;

    @Autowired
    private RecordService recordService;

    @Autowired
    private RelationshipService relationshipService;

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Autowired
    private DataRepository repository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private User actor;

    @BeforeEach
    void setUp() {
        this.transactionTemplate.executeWithoutResult(_ -> {
            ObjectProperty<String> titleProperty = this.repository.objectPropertyRepo.findById(TITLE)
                    .map(p -> (ObjectProperty<String>) p)
                    .orElseGet(() -> this.repository.objectPropertyRepo.saveAndFlush(new ObjectProperty<>(
                            TITLE,
                            PropertyType.STRING
                    )));
            this.ensureRecordType(RECORD_TYPE_A, titleProperty);
            this.ensureRecordType(RECORD_TYPE_B, titleProperty);
            this.ensureRecordType(OTHER_RECORD_TYPE, titleProperty);

            ResourceIdentifier userTypeId = new ResourceIdentifier(
                    BuiltinPlugin.BUILTIN_PLUGIN_NAME,
                    BuiltinLocationTypeIds.USER
            );
            LocationType userType = this.repository.locationTypeRepo.findById(userTypeId)
                    .orElseGet(() -> this.repository.locationTypeRepo.saveAndFlush(new LocationType(
                            userTypeId,
                            ManagedObjectKind.USER,
                            Set.of()
                    )));
            this.actor = this.repository.locationRepo.findAll().stream()
                    .filter(User.class::isInstance)
                    .map(User.class::cast)
                    .filter(user -> "rel_test_actor".equals(user.getName()))
                    .findFirst()
                    .orElseGet(() -> this.repository.locationRepo.saveAndFlush(
                            new User("rel_test_actor", null, userType)
                    ));
        });
    }

    private void ensureRecordType(ResourceIdentifier id, ObjectProperty<String> titleProperty) {
        if (this.repository.recordTypeRepo.findById(id).isEmpty()) {
            RecordType recordRoot = this.repository.recordTypeRepo.findById(RECORD_ROOT).orElseThrow();
            this.repository.recordTypeRepo.saveAndFlush(new RecordType(
                    id,
                    false,
                    recordRoot,
                    null,
                    null,
                    SecurityFilterUsage.SHOW_ALL,
                    Set.of(new ManagedObjectTypeProperty<>(titleProperty, null))
            ));
        }
    }

    @Test
    void locationToLocationMemberOfAndReportsTo() {
        LocationResponse alice = this.locationService.create(new NewLocationRequest(USER_TYPE, null, "alice", Map.of()));
        LocationResponse bob = this.locationService.create(new NewLocationRequest(USER_TYPE, null, "bob", Map.of()));
        LocationResponse engineering = this.locationService.create(new NewLocationRequest(GROUP_TYPE, null, "Engineering", Map.of()));

        ResourceIdentifier memberOf = new ResourceIdentifier(
                BuiltinPlugin.BUILTIN_PLUGIN_NAME,
                BuiltinRelationshipTypeIds.MEMBER_OF
        );
        ResourceIdentifier reportsTo = new ResourceIdentifier(
                BuiltinPlugin.BUILTIN_PLUGIN_NAME,
                BuiltinRelationshipTypeIds.REPORTS_TO
        );

        RelationshipResponse membership = this.relationshipService.create(
                this.actor,
                alice.id(),
                new NewRelationshipRequest(engineering.id(), memberOf, null)
        );
        assertEquals(memberOf, membership.typeId());
        assertEquals(ManagedObjectKind.USER, membership.sourceKind());
        assertEquals(ManagedObjectKind.GROUP, membership.targetKind());

        RelationshipResponse reports = this.relationshipService.create(
                this.actor,
                alice.id(),
                new NewRelationshipRequest(bob.id(), reportsTo, null)
        );

        List<RelationshipResponse> aliceOutgoing = this.relationshipService.list(
                this.actor,
                alice.id(),
                RelationshipDirection.OUTGOING,
                null
        );
        assertEquals(2, aliceOutgoing.size());

        List<RelationshipResponse> engineeringMembers = this.relationshipService.list(
                this.actor,
                engineering.id(),
                RelationshipDirection.INCOMING,
                memberOf
        );
        assertEquals(1, engineeringMembers.size());
        assertEquals(alice.id(), engineeringMembers.getFirst().sourceId());

        List<RelationshipResponse> bobReports = this.relationshipService.list(
                this.actor,
                bob.id(),
                RelationshipDirection.INCOMING,
                reportsTo
        );
        assertEquals(1, bobReports.size());
        assertEquals(reports.id(), bobReports.getFirst().id());

        assertThrows(ApiException.class, () -> this.relationshipService.create(
                this.actor,
                alice.id(),
                new NewRelationshipRequest(
                        this.repository.userRepo.findByName("admin").orElseThrow().getId(),
                        reportsTo,
                        null
                )
        ));

        assertThrows(ResourceAlreadyExistsException.class, () -> this.relationshipService.create(
                this.actor,
                alice.id(),
                new NewRelationshipRequest(engineering.id(), memberOf, null)
        ));

        this.relationshipService.end(this.actor, alice.id(), membership.id());
        assertTrue(this.relationshipService.list(this.actor, alice.id(), RelationshipDirection.OUTGOING, memberOf)
                .isEmpty());

        RelationshipResponse remembership = this.relationshipService.create(
                this.actor,
                alice.id(),
                new NewRelationshipRequest(engineering.id(), memberOf, null)
        );
        assertNotEquals(membership.id(), remembership.id());
    }

    @Test
    void recordToRecordRelatedToAndSupersedes() {
        RecordResponse alpha = this.recordService.create(new NewRecordRequest(RECORD_TYPE_A, "Alpha", Map.of()));
        RecordResponse beta = this.recordService.create(new NewRecordRequest(RECORD_TYPE_A, "Beta", Map.of()));
        RecordResponse gamma = this.recordService.create(new NewRecordRequest(RECORD_TYPE_A, "Gamma", Map.of()));

        ResourceIdentifier relatedTo = new ResourceIdentifier(
                BuiltinPlugin.BUILTIN_PLUGIN_NAME,
                BuiltinRelationshipTypeIds.RELATED_TO
        );
        ResourceIdentifier supersedes = new ResourceIdentifier(
                BuiltinPlugin.BUILTIN_PLUGIN_NAME,
                BuiltinRelationshipTypeIds.SUPERSEDES
        );

        RelationshipResponse related = this.relationshipService.create(
                this.actor,
                alpha.id(),
                new NewRelationshipRequest(beta.id(), relatedTo, null)
        );
        assertEquals(relatedTo, related.typeId());
        assertEquals(ManagedObjectKind.RECORD, related.sourceKind());

        RelationshipResponse superseded = this.relationshipService.create(
                this.actor,
                alpha.id(),
                new NewRelationshipRequest(gamma.id(), supersedes, null)
        );

        assertEquals(2, this.relationshipService.list(
                this.actor, alpha.id(), RelationshipDirection.OUTGOING, null
        ).size());

        List<RelationshipResponse> betaIncoming = this.relationshipService.list(
                this.actor, beta.id(), RelationshipDirection.INCOMING, relatedTo
        );
        assertEquals(1, betaIncoming.size());
        assertEquals(alpha.id(), betaIncoming.getFirst().sourceId());

        assertEquals(superseded.id(), this.relationshipService.list(
                this.actor, gamma.id(), RelationshipDirection.INCOMING, supersedes
        ).getFirst().id());

        assertThrows(ApiException.class, () -> this.relationshipService.create(
                this.actor,
                alpha.id(),
                new NewRelationshipRequest(beta.id(), supersedes, null)
        ));

        assertThrows(ResourceAlreadyExistsException.class, () -> this.relationshipService.create(
                this.actor,
                alpha.id(),
                new NewRelationshipRequest(beta.id(), relatedTo, null)
        ));

        this.relationshipService.end(this.actor, alpha.id(), related.id());
        assertTrue(this.relationshipService.list(this.actor, alpha.id(), RelationshipDirection.OUTGOING, relatedTo)
                .isEmpty());

        RelationshipResponse recreated = this.relationshipService.create(
                this.actor,
                alpha.id(),
                new NewRelationshipRequest(beta.id(), relatedTo, null)
        );
        assertNotEquals(related.id(), recreated.id());
    }

    @Test
    void locationRecordBothDirectionsCustodianAndCreatedBy() {
        LocationResponse alice = this.locationService.create(
                new NewLocationRequest(USER_TYPE, null, "alice_custodian", Map.of())
        );
        RecordResponse memo = this.recordService.create(new NewRecordRequest(RECORD_TYPE_A, "Memo", Map.of()));

        ResourceIdentifier custodianOf = new ResourceIdentifier(
                BuiltinPlugin.BUILTIN_PLUGIN_NAME,
                BuiltinRelationshipTypeIds.CUSTODIAN_OF
        );
        ResourceIdentifier createdBy = new ResourceIdentifier(
                BuiltinPlugin.BUILTIN_PLUGIN_NAME,
                BuiltinRelationshipTypeIds.CREATED_BY
        );

        RelationshipResponse custody = this.relationshipService.create(
                this.actor,
                alice.id(),
                new NewRelationshipRequest(memo.id(), custodianOf, null)
        );
        assertEquals(custodianOf, custody.typeId());
        assertEquals(alice.id(), custody.sourceId());
        assertEquals(memo.id(), custody.targetId());
        assertEquals(ManagedObjectKind.USER, custody.sourceKind());
        assertEquals(ManagedObjectKind.RECORD, custody.targetKind());

        RelationshipResponse authorship = this.relationshipService.create(
                this.actor,
                memo.id(),
                new NewRelationshipRequest(alice.id(), createdBy, null)
        );
        assertEquals(createdBy, authorship.typeId());
        assertEquals(ManagedObjectKind.RECORD, authorship.sourceKind());
        assertEquals(ManagedObjectKind.USER, authorship.targetKind());

        assertEquals(1, this.relationshipService.list(
                this.actor, alice.id(), RelationshipDirection.OUTGOING, null
        ).size());
        assertEquals(authorship.id(), this.relationshipService.list(
                this.actor, alice.id(), RelationshipDirection.INCOMING, createdBy
        ).getFirst().id());
        assertEquals(authorship.id(), this.relationshipService.list(
                this.actor, memo.id(), RelationshipDirection.OUTGOING, null
        ).getFirst().id());
        assertEquals(custody.id(), this.relationshipService.list(
                this.actor, memo.id(), RelationshipDirection.INCOMING, custodianOf
        ).getFirst().id());

        assertThrows(ResourceAlreadyExistsException.class, () -> this.relationshipService.create(
                this.actor,
                alice.id(),
                new NewRelationshipRequest(memo.id(), custodianOf, null)
        ));

        this.relationshipService.end(this.actor, memo.id(), custody.id());
        assertTrue(this.relationshipService.list(this.actor, alice.id(), RelationshipDirection.OUTGOING, custodianOf)
                .isEmpty());

        RelationshipResponse recreated = this.relationshipService.create(
                this.actor,
                alice.id(),
                new NewRelationshipRequest(memo.id(), custodianOf, null)
        );
        assertNotEquals(custody.id(), recreated.id());
    }

    @Test
    void wrongKindRejected() {
        LocationResponse alice = this.locationService.create(
                new NewLocationRequest(USER_TYPE, null, "alice_wrong_kind", Map.of())
        );
        RecordResponse memo = this.recordService.create(new NewRecordRequest(RECORD_TYPE_A, "Wrong Kind Memo", Map.of()));

        ResourceIdentifier custodianOf = new ResourceIdentifier(
                BuiltinPlugin.BUILTIN_PLUGIN_NAME,
                BuiltinRelationshipTypeIds.CUSTODIAN_OF
        );
        ResourceIdentifier createdBy = new ResourceIdentifier(
                BuiltinPlugin.BUILTIN_PLUGIN_NAME,
                BuiltinRelationshipTypeIds.CREATED_BY
        );
        ResourceIdentifier memberOf = new ResourceIdentifier(
                BuiltinPlugin.BUILTIN_PLUGIN_NAME,
                BuiltinRelationshipTypeIds.MEMBER_OF
        );

        ApiException custodianFromRecord = assertThrows(ApiException.class, () -> this.relationshipService.create(
                this.actor,
                memo.id(),
                new NewRelationshipRequest(alice.id(), custodianOf, null)
        ));
        assertEquals("validation_failed", custodianFromRecord.getError().code());
        assertTrue(custodianFromRecord.getFieldErrors().containsKey("typeId"));

        ApiException createdByFromLocation = assertThrows(ApiException.class, () -> this.relationshipService.create(
                this.actor,
                alice.id(),
                new NewRelationshipRequest(memo.id(), createdBy, null)
        ));
        assertEquals("validation_failed", createdByFromLocation.getError().code());

        ApiException locationTypeOnRecord = assertThrows(ApiException.class, () -> this.relationshipService.create(
                this.actor,
                memo.id(),
                new NewRelationshipRequest(alice.id(), memberOf, null)
        ));
        assertEquals("validation_failed", locationTypeOnRecord.getError().code());
    }

    @Test
    void typeMismatchAndCustomTypes() {
        LocationResponse alice = this.locationService.create(
                new NewLocationRequest(USER_TYPE, null, "alice_sponsors", Map.of())
        );
        LocationResponse engineering = this.locationService.create(
                new NewLocationRequest(GROUP_TYPE, null, "Sponsors", Map.of())
        );
        LocationResponse renamed = this.locationService.update(
                this.actor,
                engineering.id(),
                new UpdateLocationRequest(null, null, "Sponsors Renamed", null)
        );
        assertEquals("Sponsors Renamed", renamed.name());

        ResourceIdentifier sponsorsId = ResourceIdentifier.valueOf("test:sponsors");
        RelationshipTypeResponse createdType = this.relationshipTypeService.create(
                new NewRelationshipTypeRequest(
                        sponsorsId,
                        USER_TYPE,
                        GROUP_TYPE,
                        false
                )
        );
        assertEquals(sponsorsId, createdType.id());
        assertEquals(USER_TYPE, createdType.sourceTypeId());
        assertEquals(GROUP_TYPE, createdType.targetTypeId());

        assertTrue(this.relationshipTypeService.getAll().stream().anyMatch(t -> t.id().equals(sponsorsId)));
        assertEquals(sponsorsId, this.relationshipTypeService.get(sponsorsId).id());

        ApiException typeMismatch = assertThrows(ApiException.class, () -> this.relationshipService.create(
                this.actor,
                engineering.id(),
                new NewRelationshipRequest(alice.id(), sponsorsId, null)
        ));
        assertEquals("validation_failed", typeMismatch.getError().code());
        assertTrue(typeMismatch.getFieldErrors().containsKey("typeId"));

        RelationshipResponse sponsorship = this.relationshipService.create(
                this.actor,
                alice.id(),
                new NewRelationshipRequest(engineering.id(), sponsorsId, null)
        );
        assertEquals(sponsorsId, sponsorship.typeId());

        RecordResponse typedA = this.recordService.create(new NewRecordRequest(RECORD_TYPE_A, "Typed A", Map.of()));
        RecordResponse typedB = this.recordService.create(new NewRecordRequest(RECORD_TYPE_B, "Typed B", Map.of()));
        ResourceIdentifier aToB = ResourceIdentifier.valueOf("test:a_to_b");
        this.relationshipTypeService.create(
                new NewRelationshipTypeRequest(
                        aToB,
                        RECORD_TYPE_A,
                        RECORD_TYPE_B,
                        false
                )
        );

        ApiException recordTypeMismatch = assertThrows(ApiException.class, () -> this.relationshipService.create(
                this.actor,
                typedB.id(),
                new NewRelationshipRequest(typedA.id(), aToB, null)
        ));
        assertEquals("validation_failed", recordTypeMismatch.getError().code());

        assertEquals(aToB, this.relationshipService.create(
                this.actor,
                typedA.id(),
                new NewRelationshipRequest(typedB.id(), aToB, null)
        ).typeId());

        ResourceIdentifier anyToAny = ResourceIdentifier.valueOf("test:any_to_any");
        this.relationshipTypeService.create(
                new NewRelationshipTypeRequest(
                        anyToAny,
                        RECORD_ROOT,
                        RECORD_ROOT,
                        false
                )
        );
        ApiException selfLink = assertThrows(ApiException.class, () -> this.relationshipService.create(
                this.actor,
                typedA.id(),
                new NewRelationshipRequest(typedA.id(), anyToAny, null)
        ));
        assertEquals("validation_failed", selfLink.getError().code());
        assertTrue(selfLink.getFieldErrors().containsKey("targetId"));
    }

    @Test
    void uniquePerSourceAndLocationRecordTypeConstraints() {
        LocationResponse alice = this.locationService.create(
                new NewLocationRequest(USER_TYPE, null, "alice_custom_lrr", Map.of())
        );
        LocationResponse group = this.locationService.create(
                new NewLocationRequest(GROUP_TYPE, null, "group_custom_lrr", Map.of())
        );
        RecordResponse memo = this.recordService.create(new NewRecordRequest(RECORD_TYPE_A, "Custom Memo", Map.of()));
        RecordResponse otherMemo = this.recordService.create(
                new NewRecordRequest(OTHER_RECORD_TYPE, "Other Type Memo", Map.of())
        );

        ResourceIdentifier customId = ResourceIdentifier.valueOf("test:user_holds");
        RelationshipTypeResponse createdType = this.relationshipTypeService.create(
                new NewRelationshipTypeRequest(
                        customId,
                        USER_TYPE,
                        RECORD_TYPE_A,
                        true
                )
        );
        assertEquals(customId, createdType.id());
        assertTrue(createdType.uniquePerSource());

        RelationshipResponse edge = this.relationshipService.create(
                this.actor,
                alice.id(),
                new NewRelationshipRequest(memo.id(), customId, null)
        );
        assertEquals(customId, edge.typeId());

        RecordResponse other = this.recordService.create(new NewRecordRequest(RECORD_TYPE_A, "Other Memo", Map.of()));
        ApiException unique = assertThrows(ApiException.class, () -> this.relationshipService.create(
                this.actor,
                alice.id(),
                new NewRelationshipRequest(other.id(), customId, null)
        ));
        assertEquals("validation_failed", unique.getError().code());

        ApiException wrongLocationType = assertThrows(ApiException.class, () -> this.relationshipService.create(
                this.actor,
                group.id(),
                new NewRelationshipRequest(memo.id(), customId, null)
        ));
        assertEquals("validation_failed", wrongLocationType.getError().code());

        ApiException wrongRecordType = assertThrows(ApiException.class, () -> this.relationshipService.create(
                this.actor,
                alice.id(),
                new NewRelationshipRequest(otherMemo.id(), customId, null)
        ));
        assertEquals("validation_failed", wrongRecordType.getError().code());
    }

    @Test
    void containedByTargetsBuiltinContainerAndIsUniquePerSource() {
        ResourceIdentifier containerType = new ResourceIdentifier(
                BuiltinPlugin.BUILTIN_PLUGIN_NAME,
                BuiltinRecordTypeIds.CONTAINER
        );
        ResourceIdentifier containedBy = new ResourceIdentifier(
                BuiltinPlugin.BUILTIN_PLUGIN_NAME,
                BuiltinRelationshipTypeIds.CONTAINED_BY
        );

        assertTrue(this.repository.recordTypeRepo.existsById(containerType));
        RelationshipTypeResponse type = this.relationshipTypeService.get(containedBy);
        assertEquals(containerType, type.targetTypeId());
        assertTrue(type.uniquePerSource());

        RecordResponse folder = this.recordService.create(new NewRecordRequest(containerType, "Box A", Map.of()));
        RecordResponse otherFolder = this.recordService.create(new NewRecordRequest(containerType, "Box B", Map.of()));
        RecordResponse document = this.recordService.create(new NewRecordRequest(RECORD_TYPE_A, "Document", Map.of()));

        RelationshipResponse edge = this.relationshipService.create(
                this.actor,
                document.id(),
                new NewRelationshipRequest(folder.id(), containedBy, null)
        );
        assertEquals(containedBy, edge.typeId());
        assertEquals(folder.id(), edge.targetId());

        assertThrows(ApiException.class, () -> this.relationshipService.create(
                this.actor,
                document.id(),
                new NewRelationshipRequest(otherFolder.id(), containedBy, null)
        ));

        assertThrows(ApiException.class, () -> this.relationshipService.create(
                this.actor,
                document.id(),
                new NewRelationshipRequest(
                        this.recordService.create(new NewRecordRequest(RECORD_TYPE_A, "Not a container", Map.of())).id(),
                        containedBy,
                        null
                )
        ));
    }

    @Test
    void builtinTypesAreImmutable() {
        ResourceIdentifier newBuiltinId = new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, "not_a_real_type");
        assertThrows(BuiltinResourceImmutableException.class, () -> this.relationshipTypeService.create(
                new NewRelationshipTypeRequest(
                        newBuiltinId,
                        LOCATION_TYPE,
                        LOCATION_TYPE,
                        true
                )
        ));

        ResourceIdentifier reportsTo = new ResourceIdentifier(
                BuiltinPlugin.BUILTIN_PLUGIN_NAME,
                BuiltinRelationshipTypeIds.REPORTS_TO
        );
        assertThrows(BuiltinResourceImmutableException.class, () -> this.relationshipTypeService.update(
                reportsTo,
                new UpdateRelationshipTypeRequest(USER_TYPE, null, null)
        ));

        ResourceIdentifier relatedTo = new ResourceIdentifier(
                BuiltinPlugin.BUILTIN_PLUGIN_NAME,
                BuiltinRelationshipTypeIds.RELATED_TO
        );
        assertThrows(BuiltinResourceImmutableException.class, () -> this.relationshipTypeService.update(
                relatedTo,
                new UpdateRelationshipTypeRequest(RECORD_TYPE_A, null, null)
        ));

        ResourceIdentifier custodianOf = new ResourceIdentifier(
                BuiltinPlugin.BUILTIN_PLUGIN_NAME,
                BuiltinRelationshipTypeIds.CUSTODIAN_OF
        );
        assertThrows(BuiltinResourceImmutableException.class, () -> this.relationshipTypeService.update(
                custodianOf,
                new UpdateRelationshipTypeRequest(USER_TYPE, null, null)
        ));
    }
}
