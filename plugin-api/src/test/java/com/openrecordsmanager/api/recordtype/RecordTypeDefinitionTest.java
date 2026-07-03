package com.openrecordsmanager.api.recordtype;

import com.openrecordsmanager.api.Component;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RecordTypeDefinitionTest {

    @Test
    void testDeserialization() {
        RecordTypeDefinition type = Component.fromJson("test_record_type.json", RecordTypeDefinition.class);
        RecordTypeDefinition codeType = RecordTypeDefinition.builder("Test record type")
                .description("Test record description")
                .allowedContentTypes("application/json")
                .build();

        Assertions.assertEquals(type, codeType, "Objects should be equal");

        RecordTypeDefinition otherType = RecordTypeDefinition.builder("Not the test record type")
                .description("Test record description")
                .build();
        
        Assertions.assertNotEquals(type, otherType, "Objects should not be equal");
    }

}