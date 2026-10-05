package com.openrecordsmanager.record;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.audit.AuditEntityType;
import com.openrecordsmanager.api.audit.AuditOperation;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.builtin.BuiltinPlugin;
import com.openrecordsmanager.api.builtin.BuiltinPropertyIds;
import com.openrecordsmanager.api.template.list.IListElement;
import com.openrecordsmanager.api.template.property.PropertyType;
import com.openrecordsmanager.api.template.recordtype.SecurityFilterUsage;
import com.openrecordsmanager.audit.AuditPolicyService;
import com.openrecordsmanager.audit.AuditTestSupport;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.list.ListElement;
import com.openrecordsmanager.list.ListType;
import com.openrecordsmanager.location.user.User;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.record.dto.NewRecordRequest;
import com.openrecordsmanager.record.dto.RecordResponse;
import com.openrecordsmanager.record.dto.UpdateRecordRequest;
import com.openrecordsmanager.record.type.RecordType;
import com.openrecordsmanager.record.type.RecordTypeProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class RecordUpdateIntegrationTest {

    private static final ResourceIdentifier TITLE =
            new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinPropertyIds.TITLE);
    private static final ResourceIdentifier TEST_RECORD_TYPE = ResourceIdentifier.valueOf("test:update_record_type");
    private static final ResourceIdentifier LIST_ID = ResourceIdentifier.valueOf("test:record_list_prop_list");
    private static final ResourceIdentifier ELEMENT_A = ResourceIdentifier.valueOf("test:record_list_a");
    private static final ResourceIdentifier ELEMENT_B = ResourceIdentifier.valueOf("test:record_list_b");
    private static final ResourceIdentifier LIST_ITEM_PROP = ResourceIdentifier.valueOf("test:record_list_item_prop");
    private static final ResourceIdentifier LIST_MULTI_PROP = ResourceIdentifier.valueOf("test:record_list_multi_prop");
    private static final ResourceIdentifier LIST_RECORD_TYPE = ResourceIdentifier.valueOf("test:list_prop_record_type");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        com.openrecordsmanager.database.SqliteTestSupport.registerPrimaryMemoryDatabase(registry, RecordUpdateIntegrationTest.class);
        registry.add(BuiltinConfigs.PLUGINS_SKIP_SYNC.key(), () -> "true");
        registry.add(BuiltinConfigs.COOKIE_SECURE.key(), () -> "false");
    }

    @Autowired
    private RecordService recordService;

    @Autowired
    private DataRepository repository;

    @Autowired
    private AuditPolicyService auditPolicyService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private User admin;

    @BeforeEach
    void setUpRecordType() {
        this.admin = this.repository.userRepo.findByName("admin").orElseThrow();
        this.auditPolicyService.updatePolicy(AuditEntityType.RECORD, AuditOperation.CREATE, true, false);
        this.auditPolicyService.updatePolicy(AuditEntityType.RECORD, AuditOperation.UPDATE, true, false);
        this.auditPolicyService.updatePolicy(AuditEntityType.RECORD, AuditOperation.READ, true, false);

        this.transactionTemplate.executeWithoutResult(_ -> {
            if (this.repository.recordTypeRepo.findById(TEST_RECORD_TYPE).isEmpty()) {
                ObjectProperty<String> titleProperty = this.repository.objectPropertyRepo.findById(TITLE)
                        .map(p -> (ObjectProperty<String>) p)
                        .orElseGet(() -> this.repository.objectPropertyRepo.saveAndFlush(new ObjectProperty<>(
                                TITLE,
                                PropertyType.STRING
                        )));
                RecordType recordType = new RecordType(TEST_RECORD_TYPE, null,
                        null,
                        SecurityFilterUsage.SHOW_ALL,
                        Set.of(new RecordTypeProperty<>(titleProperty, null))
                );
                this.repository.recordTypeRepo.saveAndFlush(recordType);
            }

            ListType listType = this.repository.listTypeRepo.findById(LIST_ID).orElseGet(() ->
                    this.repository.listTypeRepo.saveAndFlush(new ListType(LIST_ID))
            );

            this.repository.listElementRepo.findById(ELEMENT_A).orElseGet(() ->
                    this.repository.listElementRepo.saveAndFlush(
                            new ListElement(ELEMENT_A, listType, 1, null, Set.of())
                    )
            );
            this.repository.listElementRepo.findById(ELEMENT_B).orElseGet(() ->
                    this.repository.listElementRepo.saveAndFlush(
                            new ListElement(ELEMENT_B, listType, 2, null, Set.of())
                    )
            );

            ObjectProperty<IListElement> listItemProp = this.repository.objectPropertyRepo.findById(LIST_ITEM_PROP)
                    .map(p -> (ObjectProperty<IListElement>) p)
                    .orElseGet(() -> this.repository.objectPropertyRepo.saveAndFlush(new ObjectProperty<>(
                            LIST_ITEM_PROP,
                            PropertyType.LIST_ITEM,
                            listType,
                            null,
                            null,
                            null,
                            false
                    )));

            ObjectProperty<Collection<IListElement>> listMultiProp = this.repository.objectPropertyRepo.findById(LIST_MULTI_PROP)
                    .map(p -> (ObjectProperty<Collection<IListElement>>) p)
                    .orElseGet(() -> this.repository.objectPropertyRepo.saveAndFlush(new ObjectProperty<>(
                            LIST_MULTI_PROP,
                            PropertyType.LIST_MULTIPLE,
                            listType,
                            null,
                            null,
                            null,
                            false
                    )));

            if (this.repository.recordTypeRepo.findById(LIST_RECORD_TYPE).isEmpty()) {
                ObjectProperty<String> titleProperty = this.repository.objectPropertyRepo.findById(TITLE)
                        .map(p -> (ObjectProperty<String>) p)
                        .orElseThrow();

                Set<RecordTypeProperty<?>> properties = new LinkedHashSet<>();
                properties.add(new RecordTypeProperty<>(titleProperty, null));
                properties.add(new RecordTypeProperty<>(listItemProp, null));
                properties.add(new RecordTypeProperty<>(listMultiProp, null));

                RecordType recordType = new RecordType(
                        LIST_RECORD_TYPE,
                        null,
                        null,
                        SecurityFilterUsage.SHOW_ALL,
                        properties
                );
                this.repository.recordTypeRepo.saveAndFlush(recordType);
            }
        });
    }

    @Test
    void createAndUpdateRecordTitle() throws InterruptedException {
        String dateModifiedKey = new ResourceIdentifier(
                BuiltinPlugin.BUILTIN_PLUGIN_NAME,
                BuiltinPropertyIds.DATE_MODIFIED
        ).toString();

        RecordResponse created = AuditTestSupport.withAudit(this.admin, () -> this.recordService.create(
                new NewRecordRequest(TEST_RECORD_TYPE, "Initial title", Map.of())
        ));
        assertEquals("Initial title", created.properties().get(TITLE.toString()).asString());
        assertNotNull(created.properties().get(dateModifiedKey));
        Instant createdModified = Instant.parse(created.properties().get(dateModifiedKey).asString());

        // Instant.now() may share the same millisecond; ensure a later timestamp.
        Thread.sleep(5);

        RecordResponse updated = AuditTestSupport.withAudit(this.admin, () -> this.recordService.update(
                this.admin,
                created.id(),
                new UpdateRecordRequest(null, "Updated title", null)
        ));
        assertEquals("Updated title", updated.properties().get(TITLE.toString()).asString());
        assertNotNull(updated.properties().get(dateModifiedKey));
        Instant updatedModified = Instant.parse(updated.properties().get(dateModifiedKey).asString());
        assertTrue(
                updatedModified.isAfter(createdModified),
                () -> "date_modified should advance after update: before=" + createdModified
                        + " after=" + updatedModified
        );

        RecordResponse loaded = this.recordService.get(this.admin, created.id());
        assertEquals("Updated title", loaded.properties().get(TITLE.toString()).asString());
        Instant loadedModified = Instant.parse(loaded.properties().get(dateModifiedKey).asString());
        // SQLite TIMESTAMP storage is millisecond precision.
        assertEquals(updatedModified.truncatedTo(ChronoUnit.MILLIS), loadedModified.truncatedTo(ChronoUnit.MILLIS));
    }

    @Test
    void createAndUpdateRecordListItemProperties() {
        Map<ResourceIdentifier, JsonNode> createProps = Map.of(
                LIST_ITEM_PROP, JsonNodeFactory.instance.stringNode(ELEMENT_A.toString()),
                LIST_MULTI_PROP, JsonNodeFactory.instance.arrayNode().add(ELEMENT_B.toString())
        );

        RecordResponse created = AuditTestSupport.withAudit(this.admin, () -> this.recordService.create(
                new NewRecordRequest(LIST_RECORD_TYPE, "List record", createProps)
        ));
        assertEquals(ELEMENT_A.toString(), created.properties().get(LIST_ITEM_PROP.toString()).asString());
        assertEquals(ELEMENT_B.toString(), created.properties().get(LIST_MULTI_PROP.toString()).get(0).asString());

        Map<ResourceIdentifier, JsonNode> updateProps = Map.of(
                LIST_ITEM_PROP, JsonNodeFactory.instance.stringNode(ELEMENT_B.toString()),
                LIST_MULTI_PROP, JsonNodeFactory.instance.arrayNode()
                        .add(ELEMENT_A.toString())
                        .add(ELEMENT_B.toString())
        );

        RecordResponse updated = AuditTestSupport.withAudit(this.admin, () -> this.recordService.update(
                this.admin,
                created.id(),
                new UpdateRecordRequest(null, null, updateProps)
        ));
        assertEquals(ELEMENT_B.toString(), updated.properties().get(LIST_ITEM_PROP.toString()).asString());
        assertEquals(ELEMENT_A.toString(), updated.properties().get(LIST_MULTI_PROP.toString()).get(0).asString());
        assertEquals(ELEMENT_B.toString(), updated.properties().get(LIST_MULTI_PROP.toString()).get(1).asString());

        RecordResponse loaded = this.recordService.get(this.admin, created.id());
        assertEquals(ELEMENT_B.toString(), loaded.properties().get(LIST_ITEM_PROP.toString()).asString());
        assertEquals(ELEMENT_A.toString(), loaded.properties().get(LIST_MULTI_PROP.toString()).get(0).asString());
        assertEquals(ELEMENT_B.toString(), loaded.properties().get(LIST_MULTI_PROP.toString()).get(1).asString());
    }

}
