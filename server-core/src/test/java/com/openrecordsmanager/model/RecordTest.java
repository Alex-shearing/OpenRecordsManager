package com.openrecordsmanager.model;

import com.openrecordsmanager.api.property.PropertyType;
import com.openrecordsmanager.api.recordtype.SecurityFilterUsage;
import com.openrecordsmanager.resources.ExpressionsService;
import com.openrecordsmanager.resources.ResourceIdentifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashSet;
import java.util.UUID;

@SpringBootTest
class RecordTest {

    // Construct test user
    private static final User TEST_USER = new User(UUID.randomUUID());

    static {
        // Number property
        ObjectProperty<Long> numberProperty = new ObjectProperty<>(
                ResourceIdentifier.valueOf("test:number_property"),
                "Number property",
                "Number property",
                PropertyType.NUMBER
        );
        TEST_USER.setProperty(numberProperty, 10L);

        // String property
        ObjectProperty<String> stringProperty = new ObjectProperty<>(
                ResourceIdentifier.valueOf("test:string_property"),
                "String property",
                "String property",
                PropertyType.STRING
        );
        TEST_USER.setProperty(stringProperty, "test value");
    }

    @Autowired
    private ExpressionsService expressionsService;

    @Test
    void securityFilter_properties() {
        ObjectProperty<String> stringProperty = new ObjectProperty<>(
                ResourceIdentifier.valueOf("test:user_string_property"),
                "String property",
                "String property",
                PropertyType.STRING
        );
        stringProperty.securityFilter = "value == principal['test:string_property']";

        RecordType recordType = new RecordType(
                ResourceIdentifier.valueOf("test:record_type"),
                "Record type",
                "Record type",
                null,
                null,
                SecurityFilterUsage.HIDE_RECORD,
                new HashSet<>()
        );
        recordType.properties.add(new RecordTypeProperty<>(stringProperty, null));

        Record record = new Record("Record", recordType);
        record.setProperty(stringProperty, "test value");

        Assertions.assertEquals(SecurityFilterUsage.SHOW_ALL, record.securityFilter(this.expressionsService, TEST_USER, record), "User should have access");

        record.setProperty(stringProperty, "other value");

        Assertions.assertEquals(SecurityFilterUsage.HIDE_RECORD, record.securityFilter(this.expressionsService, TEST_USER, record), "User should not have access");
    }

    @Test
    void securityFilter_recordType() {
        RecordType recordType = new RecordType(
                ResourceIdentifier.valueOf("test:record_type"),
                "Record type",
                "Record type",
                null,
                "principal['test:string_property'] == 'not this'",
                SecurityFilterUsage.HIDE_RECORD,
                new HashSet<>()
        );

        Record record = new Record("Record", recordType);

        Assertions.assertEquals(SecurityFilterUsage.HIDE_RECORD, record.securityFilter(this.expressionsService, TEST_USER, record), "User should not have access");

        Assertions.assertNotEquals(SecurityFilterUsage.SHOW_ALL, record.securityFilter(this.expressionsService, TEST_USER, record), "User should not have access");
        Assertions.assertNotEquals(SecurityFilterUsage.HIDE_FILES, record.securityFilter(this.expressionsService, TEST_USER, record), "User should not have access");
        Assertions.assertNotEquals(SecurityFilterUsage.HIDE_METADATA_AND_FILES, record.securityFilter(this.expressionsService, TEST_USER, record), "User should not have access");
    }
}