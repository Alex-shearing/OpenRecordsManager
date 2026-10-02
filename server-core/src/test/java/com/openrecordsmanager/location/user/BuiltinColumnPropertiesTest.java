package com.openrecordsmanager.location.user;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.builtin.BuiltinPlugin;
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

    private static final ResourceIdentifier TITLE =
            new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinPropertyIds.TITLE);
    private static final ResourceIdentifier NOTES =
            new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinPropertyIds.NOTES);
    private static final ResourceIdentifier GIVEN_NAME =
            new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinPropertyIds.GIVEN_NAME);
    private static final ResourceIdentifier DATE_CREATED =
            new ResourceIdentifier(BuiltinPlugin.BUILTIN_PLUGIN_NAME, BuiltinPropertyIds.DATE_CREATED);

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
        ObjectProperty<String> titleProperty = requireProperty(TITLE);
        ObjectProperty<String> notesProperty = requireProperty(NOTES);
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
        assertEquals("default notes", record.toWireMap().get(NOTES.toString()).asString());
        assertEquals("tba", record.toWireMap().get(TITLE.toString()).asString());
    }

    @Test
    void userStoresBuiltinPropertiesInColumnsNotEavMap() {
        ObjectProperty<String> givenNameProperty = requireProperty(GIVEN_NAME);

        User user = new User("test_user", null);
        user.setProperty(givenNameProperty, "Ada");

        assertEquals("Ada", user.getProperty(givenNameProperty));
        assertEquals("Ada", user.toWireMap().get(GIVEN_NAME.toString()).asString());
        assertNotNull(user.toWireMap().get(DATE_CREATED.toString()));
    }

    @Test
    void setPropertyUpdatesDateModifiedForBuiltinAndDynamicProperties() {
        ObjectProperty<String> givenNameProperty = requireProperty(GIVEN_NAME);
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
        ObjectProperty<String> notesProperty = requireProperty(NOTES);
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
