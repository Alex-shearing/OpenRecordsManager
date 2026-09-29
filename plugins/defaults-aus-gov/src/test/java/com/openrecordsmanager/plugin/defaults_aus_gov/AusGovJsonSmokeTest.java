package com.openrecordsmanager.plugin.defaults_aus_gov;

import com.openrecordsmanager.api.template.TemplateComponent;
import com.openrecordsmanager.api.template.list.ListTemplate;
import com.openrecordsmanager.api.template.property.ObjectPropertyTemplate;
import com.openrecordsmanager.api.template.recordtype.RecordTypeTemplate;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class AusGovJsonSmokeTest {

    @Test
    void loadsSecurityClassification() {
        ListTemplate list = load("list/security_classification.json", ListTemplate.class);
        assertTrue(list.defaultEntries().containsKey("official_sensitive"));
        assertEquals(0, list.defaultEntries().get("unofficial").index());
    }

    @Test
    void loadsRecordProperty() {
        ObjectPropertyTemplate<?> prop = load(
                "object_property/record_security_classification.json",
                ObjectPropertyTemplate.class
        );
        assertNotNull(prop.listType());
        assertNotNull(prop.securityFilter());
    }

    @Test
    void loadsEmailRecordType() {
        RecordTypeTemplate type = load("record_type/email_record_type.json", RecordTypeTemplate.class);
        assertFalse(type.properties().isEmpty());
        assertNotNull(type.allowedContentTypes());
        assertFalse(type.allowedContentTypes().isEmpty());
    }

    @Test
    void loadsSecurityCaveatWithoutExplicitIndex() {
        ListTemplate list = load("list/security_caveat.json", ListTemplate.class);
        assertEquals(0, list.defaultEntries().get("delicate_source").index());
        assertEquals(0, list.defaultEntries().get("cabinet").index());
    }

    @Test
    void loadsReleasabilityCountries() {
        ListTemplate list = load("list/releasability_caveat.json", ListTemplate.class);
        assertTrue(list.defaultEntries().size() > 200);
        assertTrue(list.defaultEntries().containsKey("austeo"));
        assertTrue(list.defaultEntries().containsKey("aus"));
    }

    private static <T extends TemplateComponent> T load(String resource, Class<T> type) {
        try (InputStream in = AusGovJsonSmokeTest.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalArgumentException("Missing resource: " + resource);
            }
            return TemplateComponent.fromJson(in, type);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + resource, e);
        }
    }
}
