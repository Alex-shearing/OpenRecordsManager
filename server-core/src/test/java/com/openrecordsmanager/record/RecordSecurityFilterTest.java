package com.openrecordsmanager.record;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.location.LocationKind;
import com.openrecordsmanager.api.template.property.PropertyType;
import com.openrecordsmanager.api.template.recordtype.SecurityFilterUsage;
import com.openrecordsmanager.location.type.LocationType;
import com.openrecordsmanager.location.type.LocationTypeProperty;
import com.openrecordsmanager.location.user.User;
import com.openrecordsmanager.plugin.ExpressionsService;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.record.type.RecordType;
import com.openrecordsmanager.record.type.RecordTypeProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SpringBootTest
class RecordSecurityFilterTest {

    // Construct test user
    private User testUser;

    @BeforeEach
    void setUp() {
        // Number property
        ObjectProperty<Long> numberProperty = new ObjectProperty<>(
                ResourceIdentifier.valueOf("test:number_property"),
                PropertyType.NUMBER
        );

        // String property
        ObjectProperty<String> stringProperty = new ObjectProperty<>(
                ResourceIdentifier.valueOf("test:string_property"),
                PropertyType.STRING
        );
        LocationType userType = new LocationType(
                ResourceIdentifier.valueOf("test:filter_user_type"),
                LocationKind.USER,
                Set.of(
                        new LocationTypeProperty<>(numberProperty, null),
                        new LocationTypeProperty<>(stringProperty, null)
                )
        );
        this.testUser = new User("test_user", null, userType);
        this.testUser.setProperty(numberProperty, 10L);
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