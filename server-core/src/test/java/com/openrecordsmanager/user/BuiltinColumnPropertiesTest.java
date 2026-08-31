package com.openrecordsmanager.user;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.builtin.BuiltinProperties;
import com.openrecordsmanager.api.template.property.PropertyType;
import com.openrecordsmanager.api.template.recordtype.SecurityFilterUsage;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.record.Record;
import com.openrecordsmanager.recordtype.RecordType;
import com.openrecordsmanager.recordtype.RecordTypeProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuiltinColumnPropertiesTest {

    @BeforeEach
    void setUpUserColumnRegistry() {
        UserBuiltinColumnPropertyRegistry.initForTest(Set.of(
                new ObjectProperty<>(BuiltinProperties.DATE_CREATED_ID, "Date Created", "Date Created", PropertyType.DATE),
                new ObjectProperty<>(BuiltinProperties.DATE_MODIFIED_ID, "Date Modified", "Date Modified", PropertyType.DATE),
                new ObjectProperty<>(BuiltinProperties.GIVEN_NAME_ID, "Given Name", "Given Name", PropertyType.STRING),
                new ObjectProperty<>(BuiltinProperties.SURNAME_ID, "Surname", "Surname", PropertyType.STRING),
                new ObjectProperty<>(BuiltinProperties.HONORIFIC_ID, "Honorific", "Honorific", PropertyType.STRING),
                new ObjectProperty<>(BuiltinProperties.EMAIL_ID, "Email", "Email", PropertyType.STRING)
        ));
    }

    @Test
    void recordStoresBuiltinPropertiesInColumnsNotEavMap() {
        ObjectProperty<String> titleProperty = new ObjectProperty<>(
                BuiltinProperties.TITLE_ID,
                "Title",
                "Title",
                PropertyType.STRING
        );
        ObjectProperty<String> notesProperty = new ObjectProperty<>(
                BuiltinProperties.NOTES_ID,
                "Notes",
                "Notes",
                PropertyType.STRING
        );
        RecordType recordType = new RecordType(
                ResourceIdentifier.valueOf("test:record_type"),
                "Record type",
                "Record type",
                null,
                null,
                SecurityFilterUsage.SHOW_ALL,
                Set.of(
                        new RecordTypeProperty<>(notesProperty, "default notes"),
                        new RecordTypeProperty<>(titleProperty, null)
                )
        );

        Record record = new Record("tba", recordType);

        assertEquals("tba", record.getTitle());
        assertEquals("default notes", record.getNotes());
        assertEquals("default notes", record.getProperty(notesProperty));
        assertEquals("default notes", record.toPropertyMap(true).get(BuiltinProperties.NOTES_ID.toString()));
        assertEquals("tba", record.toPropertyMap(true).get(BuiltinProperties.TITLE_ID.toString()));
    }

    @Test
    void userStoresBuiltinPropertiesInColumnsNotEavMap() {
        ObjectProperty<String> givenNameProperty = new ObjectProperty<>(
                BuiltinProperties.GIVEN_NAME_ID,
                "Given Name",
                "Given Name",
                PropertyType.STRING
        );

        User user = new User("test_user", null);
        user.setProperty(givenNameProperty, "Ada");

        assertEquals("Ada", user.getGivenName());
        assertEquals("Ada", user.getProperty(givenNameProperty));
        assertEquals("Ada", user.toPropertyMap(true).get(BuiltinProperties.GIVEN_NAME_ID.toString()));
        assertNotNull(user.toPropertyMap(true).get(BuiltinProperties.DATE_CREATED_ID.toString()));
    }

    @Test
    void setPropertyUpdatesDateModifiedForBuiltinAndDynamicProperties() {
        ObjectProperty<String> givenNameProperty = new ObjectProperty<>(
                BuiltinProperties.GIVEN_NAME_ID,
                "Given Name",
                "Given Name",
                PropertyType.STRING
        );
        ObjectProperty<String> customProperty = new ObjectProperty<>(
                ResourceIdentifier.valueOf("test:custom_property"),
                "Custom",
                "Custom",
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
        ObjectProperty<String> notesProperty = new ObjectProperty<>(
                BuiltinProperties.NOTES_ID,
                "Notes",
                "Notes",
                PropertyType.STRING
        );
        RecordType recordType = new RecordType(
                ResourceIdentifier.valueOf("test:record_type"),
                "Record type",
                "Record type",
                null,
                null,
                SecurityFilterUsage.SHOW_ALL,
                Set.of(new RecordTypeProperty<>(notesProperty, "default notes"))
        );

        Record record = new Record("tba", recordType);
        Instant beforeChange = record.getDateModified();

        record.setProperty(notesProperty, "updated notes");

        assertEquals("updated notes", record.getNotes());
        assertTrue(record.getDateModified().isAfter(beforeChange));
    }
}
