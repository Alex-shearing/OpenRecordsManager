package com.openrecordsmanager.api.template.property;

import com.openrecordsmanager.api.template.TemplateComponent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

class ObjectPropertyTemplateTest {

    @Test
    void testDeserialisation() {
        ObjectPropertyTemplate<?> property = load("user_email_property.json", ObjectPropertyTemplate.class);
        ObjectPropertyTemplate<?> codeProperty = ObjectPropertyTemplate.builder("User Email Address", PropertyType.STRING)
                .description("An email address")
                .defaultValue("admin@company.com")
                .validator("true")
                .build();

        Assertions.assertEquals(property.name(), codeProperty.name(), "Name should be equal");
        Assertions.assertEquals(property.description(), codeProperty.description(), "Description should be equal");
        Assertions.assertEquals(property.defaultValue(), codeProperty.defaultValue(), "Default should be equal");
        Assertions.assertEquals(property.validator(), codeProperty.validator(), "Validator should be equal");

        Assertions.assertEquals(property, codeProperty, "Object should be equal");
    }

    private static <T extends TemplateComponent> T load(String resource, Class<T> type) {
        try (InputStream in = ObjectPropertyTemplateTest.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalArgumentException("Missing resource: " + resource);
            }
            return TemplateComponent.fromJson(in, type);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + resource, e);
        }
    }
}
