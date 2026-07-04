package com.openrecordsmanager.api.template.property;

import com.openrecordsmanager.api.Component;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

class PropertyDefinitionTest {

    static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void testDeserialisation() {
        try (InputStream inputStream = PropertyDefinitionTest.class
                .getClassLoader()
                .getResourceAsStream("user_email_property.json")) {

            if (inputStream == null) {
                throw new IllegalArgumentException("File not found in resources!");
            }

            PropertyDefinition<?> property = MAPPER.readValue(inputStream, PropertyDefinition.class);
            PropertyDefinition<?> codeProperty = PropertyDefinition.builder("User Email Address", PropertyType.STRING)
                    .description("An email address")
                    .defaultValue("admin@company.com")
                    .validator("true")
                    .build();

            Assertions.assertEquals(property.name(), codeProperty.name(), "Name should be equal");
            Assertions.assertEquals(property.description(), codeProperty.description(), "Description should be equal");
            Assertions.assertEquals(property.defaultValue(), codeProperty.defaultValue(), "Default should be equal");
            Assertions.assertEquals(property.validator(), codeProperty.validator(), "Validator should be equal");

            Assertions.assertEquals(property, codeProperty, "Object should be equal");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testDeserialisationFull() {
        PropertyDefinition<?> property = Component.fromJson("user_email_property.json", PropertyDefinition.class);
        PropertyDefinition<?> codeProperty = PropertyDefinition.builder("User Email Address", PropertyType.STRING)
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
}