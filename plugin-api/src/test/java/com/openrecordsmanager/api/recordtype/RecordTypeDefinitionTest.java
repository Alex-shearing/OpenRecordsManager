package com.openrecordsmanager.api.recordtype;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.recordtype.RecordTypeDefinition;
import com.openrecordsmanager.api.types.ComponentTypes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RecordTypeDefinitionTest {

    @Test
    void testDeserialization() {
        RecordTypeDefinition type = Component.fromJson("test_record_type.json", RecordTypeDefinition.class);
        RecordTypeDefinition codeType = RecordTypeDefinition.builder("Test record type")
                .description("Test record description")
                .allowedContentTypes("application/json")
                .property(ComponentReference.reference(ComponentTypes.PROPERTY, ResourceIdentifier.valueOf("test:user_email_property")))
                .property(ComponentReference.reference(ComponentTypes.PROPERTY, ResourceIdentifier.valueOf("test:user_email_property_2")))
                .build();

        Assertions.assertEquals(codeType.name(), type.name(), "Names should be equal");
        Assertions.assertEquals(codeType.allowedContentTypes(), type.allowedContentTypes(), "Content types should be equal");
        Assertions.assertEquals(codeType.description(), type.description(), "Description should be equal");
        Assertions.assertEquals(codeType.properties(), type.properties(), "Properties should be equal");
        Assertions.assertEquals(codeType, type, "Objects should be equal");

        RecordTypeDefinition otherType = RecordTypeDefinition.builder("Not the test record type")
                .description("Test record description")
                .build();

        Assertions.assertNotEquals(otherType, type, "Objects should not be equal");
    }

}