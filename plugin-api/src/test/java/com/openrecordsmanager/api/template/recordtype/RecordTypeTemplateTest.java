package com.openrecordsmanager.api.template.recordtype;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.TemplateComponent;
import com.openrecordsmanager.api.types.ComponentTypes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RecordTypeTemplateTest {

    @Test
    void testDeserialization() {
        RecordTypeTemplate type = TemplateComponent.fromJson("test_record_type.json", RecordTypeTemplate.class);
        RecordTypeTemplate codeType = RecordTypeTemplate.builder("Test record type")
                .description("Test record description")
                .allowedContentTypes("application/json")
                .property(ComponentReference.of(ComponentTypes.OBJECT_PROPERTY, ResourceIdentifier.valueOf("test:user_email_property")))
                .property(ComponentReference.of(ComponentTypes.OBJECT_PROPERTY, ResourceIdentifier.valueOf("test:user_email_property_2")))
                .build();

        Assertions.assertEquals(codeType.name(), type.name(), "Names should be equal");
        Assertions.assertEquals(codeType.allowedContentTypes(), type.allowedContentTypes(), "Content types should be equal");
        Assertions.assertEquals(codeType.description(), type.description(), "Description should be equal");
        Assertions.assertEquals(codeType.properties(), type.properties(), "Properties should be equal");
        Assertions.assertEquals(codeType, type, "Objects should be equal");

        RecordTypeTemplate otherType = RecordTypeTemplate.builder("Not the test record type")
                .description("Test record description")
                .build();

        Assertions.assertNotEquals(otherType, type, "Objects should not be equal");
    }

}