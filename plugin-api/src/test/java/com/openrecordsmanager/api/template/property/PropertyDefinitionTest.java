package com.openrecordsmanager.api.template.property;

import com.openrecordsmanager.api.Component;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

class PropertyDefinitionTest {

    static ObjectMapper MAPPER = new ObjectMapper();

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


            Assertions.assertEquals(property.getName(), codeProperty.getName(), "Name should be equal");
            Assertions.assertEquals(property.getDescription(), codeProperty.getDescription(), "Description should be equal");
            Assertions.assertEquals(property.getDefaultValue(), codeProperty.getDefaultValue(), "Default should be equal");
            Assertions.assertEquals(property.getValidator(), codeProperty.getValidator(), "Validator should be equal");

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

        Assertions.assertEquals(property.getName(), codeProperty.getName(), "Name should be equal");
        Assertions.assertEquals(property.getDescription(), codeProperty.getDescription(), "Description should be equal");
        Assertions.assertEquals(property.getDefaultValue(), codeProperty.getDefaultValue(), "Default should be equal");
        Assertions.assertEquals(property.getValidator(), codeProperty.getValidator(), "Validator should be equal");

        Assertions.assertEquals(property, codeProperty, "Object should be equal");
    }
}