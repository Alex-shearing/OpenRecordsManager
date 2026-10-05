package com.openrecordsmanager.api.audit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AuditEntityTypeTest {

    @Test
    void fromKeyRejectsUnknownKeys() {
        assertThrows(IllegalArgumentException.class, () -> AuditEntityType.fromKey("not_a_type"));
    }
}
