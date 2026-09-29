package com.openrecordsmanager.record;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.property.PropertyType;
import com.openrecordsmanager.api.template.recordtype.SecurityFilterUsage;
import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.recordtype.RecordType;
import com.openrecordsmanager.recordtype.RecordTypeProperty;
import com.openrecordsmanager.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SpringBootTest
class RecordSecurityFilterTest {

    // Construct test user
    private User testUser;

    @BeforeEach
    void setUp() {
        this.testUser = new User("test_user", null);

        // Number property
        ObjectProperty<Long> numberProperty = new ObjectProperty<>(
                ResourceIdentifier.valueOf("test:number_property"),
                PropertyType.NUMBER
        );
        this.testUser.setProperty(numberProperty, 10L);

        // String property
        ObjectProperty<String> stringProperty = new ObjectProperty<>(
                ResourceIdentifier.valueOf("test:string_property"),
                PropertyType.STRING
        );
        this.testUser.setProperty(stringProperty, "test value");
    }

    @Autowired
    private ExpressionsService expressionsService;

    @Test
    void securityFilter_properties() {
        ObjectProperty<String> stringProperty = new ObjectProperty<>(
                ResourceIdentifier.valueOf("test:user_string_property"),
                PropertyType.STRING
        );
        stringProperty.setSecurityFilter("value == principal['test:string_property']");

        RecordType recordType = new RecordType(
                ResourceIdentifier.valueOf("test:record_type"),
                null,
                null,
                SecurityFilterUsage.HIDE_RECORD,
                new HashSet<>()
        );
        recordType.getProperties().add(new RecordTypeProperty<>(stringProperty, null));

        Record record = new Record("Record", recordType);
        record.setProperty(stringProperty, "test value");

        assertEquals(SecurityFilterUsage.SHOW_ALL, record.securityFilter(this.expressionsService, this.testUser), "User should have access");

        record.setProperty(stringProperty, "other value");

        assertEquals(SecurityFilterUsage.HIDE_RECORD, record.securityFilter(this.expressionsService, this.testUser), "User should not have access");
    }

    @Test
    void securityFilter_recordType() {
        RecordType recordType = new RecordType(
                ResourceIdentifier.valueOf("test:record_type"),
                null,
                "principal['test:string_property'] == 'not this'",
                SecurityFilterUsage.HIDE_RECORD,
                new HashSet<>()
        );

        Record record = new Record("Record", recordType);

        assertEquals(SecurityFilterUsage.HIDE_RECORD, record.securityFilter(this.expressionsService, this.testUser), "User should not have access");

        assertNotEquals(SecurityFilterUsage.SHOW_ALL, record.securityFilter(this.expressionsService, this.testUser), "User should not have access");
        assertNotEquals(SecurityFilterUsage.HIDE_FILES, record.securityFilter(this.expressionsService, this.testUser), "User should not have access");
    }
}