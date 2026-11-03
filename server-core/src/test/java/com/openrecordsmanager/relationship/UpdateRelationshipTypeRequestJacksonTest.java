package com.openrecordsmanager.relationship;

import com.openrecordsmanager.relationship.dto.UpdateRelationshipTypeRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UpdateRelationshipTypeRequestJacksonTest {

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void omittedFieldsRemainNullWhileProvidedIdsDeserialize() {
        UpdateRelationshipTypeRequest omitted =
                this.jsonMapper.readValue("{\"uniquePerSource\":true}", UpdateRelationshipTypeRequest.class);
        assertNull(omitted.sourceTypeId());
        assertNull(omitted.targetTypeId());
        assertEquals(Boolean.TRUE, omitted.uniquePerSource());

        UpdateRelationshipTypeRequest valued = this.jsonMapper.readValue(
                "{\"sourceTypeId\":\"test:a\",\"targetTypeId\":\"test:b\",\"uniquePerSource\":true}",
                UpdateRelationshipTypeRequest.class
        );
        assertEquals("test:a", valued.sourceTypeId().toString());
        assertEquals("test:b", valued.targetTypeId().toString());
        assertEquals(Boolean.TRUE, valued.uniquePerSource());
    }
}
