package com.openrecordsmanager.user;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.builtin.BuiltinPropertyIds;
import com.openrecordsmanager.api.template.property.PropertyType;
import com.openrecordsmanager.api.template.recordtype.SecurityFilterUsage;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.database.SqliteTestSupport;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.record.Record;
import com.openrecordsmanager.recordtype.RecordType;
import com.openrecordsmanager.recordtype.RecordTypeProperty;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.node.JsonNodeFactory;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BuiltinColumnPropertiesTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SqliteTestSupport.registerPrimaryMemoryDatabase(registry, BuiltinColumnPropertiesTest.class);
        registry.add(BuiltinConfigs.PLUGINS_SKIP_SYNC.key(), () -> "true");
        registry.add(BuiltinConfigs.COOKIE_SECURE.key(), () -> "false");
    }

    @Autowired
    private DataRepository repository;

    @Test
    void recordStoresBuiltinPropertiesInColumnsNotEavMap() {
        ObjectProperty<String> titleProperty = requireProperty(BuiltinPropertyIds.TITLE);
        ObjectProperty<String> notesProperty = requireProperty(BuiltinPropertyIds.NOTES);
        RecordType recordType = new RecordType(
                ResourceIdentifier.valueOf("test:record_type"),
                null,
                null,
                SecurityFilterUsage.SHOW_ALL,
                Set.of(
                        new RecordTypeProperty<>(notesProperty, JsonNodeFactory.instance.stringNode("default notes")),
                        new RecordTypeProperty<>(titleProperty, null)
                )
        );

        Record record = new Record("tba", recordType);

        assertEquals("tba", record.getTitle());
        assertEquals("default notes", record.getNotes());
        assertEquals("default notes", record.getProperty(notesProperty));
        assertEquals("default notes", record.toWireMap().get(BuiltinPropertyIds.NOTES.toString()).asString());
        assertEquals("tba", record.toWireMap().get(BuiltinPropertyIds.TITLE.toString()).asString());
    }

    @Test
    void userStoresBuiltinPropertiesInColumnsNotEavMap() {
        ObjectProperty<String> givenNameProperty = requireProperty(BuiltinPropertyIds.GIVEN_NAME);

        User user = new User("test_user", null);
        user.setProperty(givenNameProperty, "Ada");

        assertEquals("Ada", user.getGivenName());
        assertEquals("Ada", user.getProperty(givenNameProperty));
        assertEquals("Ada", user.toWireMap().get(BuiltinPropertyIds.GIVEN_NAME.toString()).asString());
        assertNotNull(user.toWireMap().get(BuiltinPropertyIds.DATE_CREATED.toString()));
    }

    @Test
    void setPropertyUpdatesDateModifiedForBuiltinAndDynamicProperties() {
        ObjectProperty<String> givenNameProperty = requireProperty(BuiltinPropertyIds.GIVEN_NAME);
        ObjectProperty<String> customProperty = new ObjectProperty<>(
                ResourceIdentifier.valueOf("test:custom_property"),
                PropertyType.STRING
        );

        User user = new User("test_user", null);
        Instant createdModified = user.getDateModified();

        user.setProperty(givenNameProperty, "Ada");
        Instant afterBuiltinChange = user.getDateModified();
        assertTrue(afterBuiltinChange.isAfter(createdModified));

        user.setProperty(customProperty, "custom value");
        Instant afterDynamicChange = user.getDateModified();
        assertTrue(afterDynamicChange.isAfter(afterBuiltinChange));

        user.setProperty(givenNameProperty, "Ada");
        assertEquals(afterDynamicChange, user.getDateModified());
    }

    @Test
    void recordSetPropertyUpdatesDateModifiedForBuiltinProperties() {
        ObjectProperty<String> notesProperty = requireProperty(BuiltinPropertyIds.NOTES);
        RecordType recordType = new RecordType(
                ResourceIdentifier.valueOf("test:record_type"),
                null,
                null,
                SecurityFilterUsage.SHOW_ALL,
                Set.of(new RecordTypeProperty<>(notesProperty, JsonNodeFactory.instance.textNode("default notes")))
        );

        Record record = new Record("tba", recordType);
        Instant beforeChange = record.getDateModified();

        record.setProperty(notesProperty, "updated notes");

        assertEquals("updated notes", record.getNotes());
        assertTrue(record.getDateModified().isAfter(beforeChange));
    }

    @SuppressWarnings("unchecked")
    private <T> ObjectProperty<T> requireProperty(ResourceIdentifier id) {
        return (ObjectProperty<T>) this.repository.objectPropertyRepo.findById(id)
                .orElseThrow(() -> new IllegalStateException("Missing seeded property: " + id));
    }
}
