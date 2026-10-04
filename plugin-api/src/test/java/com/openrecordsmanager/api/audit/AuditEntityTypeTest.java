package com.openrecordsmanager.api.audit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuditEntityTypeTest {

    @Test
    void fromKeyMapsLegacyUserAndGroupToLocation() {
        assertEquals(AuditEntityType.LOCATION, AuditEntityType.fromKey("user"));
        assertEquals(AuditEntityType.LOCATION, AuditEntityType.fromKey("group"));
        assertEquals(AuditEntityType.LOCATION, AuditEntityType.fromKey("location"));
    }

    @Test
    void fromKeyRejectsUnknownKeys() {
        assertThrows(IllegalArgumentException.class, () -> AuditEntityType.fromKey("not_a_type"));
    }
}
