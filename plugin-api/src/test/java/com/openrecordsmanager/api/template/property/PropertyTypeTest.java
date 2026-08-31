package com.openrecordsmanager.api.template.property;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

class PropertyTypeTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    @Test
    void castScalarsFromJsonValues() {
        Assertions.assertEquals("hello", PropertyType.STRING.parseValue("hello"));
        Assertions.assertEquals(true, PropertyType.BOOLEAN.parseValue(true));
        Assertions.assertEquals(false, PropertyType.BOOLEAN.parseValue("false"));
        Assertions.assertEquals(42L, PropertyType.NUMBER.parseValue(42));
        Assertions.assertEquals(42L, PropertyType.NUMBER.parseValue("42"));
        Assertions.assertEquals(1.5d, PropertyType.DECIMAL.parseValue(1.5));
        Assertions.assertEquals(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                PropertyType.UUID.parseValue("00000000-0000-0000-0000-000000000001")
        );
    }

    @Test
    void castListsFromJsonValues() {
        Assertions.assertEquals(
                List.of("a", "b"),
                PropertyType.STRING_LIST.parseValue(List.of("a", "b"))
        );
        Assertions.assertEquals(
                List.of(1, 2),
                PropertyType.INT_LIST.parseValue(List.of(1, 2))
        );
    }

    @Test
    void parseValueFromJsonStrings() throws Exception {
        Assertions.assertEquals(true, PropertyType.BOOLEAN.parseValue("true"));
        Assertions.assertEquals(
                List.of("http://localhost:5173", "http://localhost:3000"),
                PropertyType.STRING_LIST.parseValue("[\"http://localhost:5173\",\"http://localhost:3000\"]")
        );
        Assertions.assertEquals("Open Records Manager", PropertyType.STRING.parseValue("\"Open Records Manager\""));
    }

    @Test
    void propertyOnlyTypesDoNotSupportConfig() {
        Assertions.assertFalse(PropertyType.CALCULATED.supportsConfig());
        Assertions.assertFalse(PropertyType.LIST_ITEM.supportsConfig());
    }

    @Test
    void jacksonDeserialisesPropertyTypeByName() throws Exception {
        PropertyType<?> type = MAPPER.readValue("\"string\"", PropertyType.class);
        Assertions.assertSame(PropertyType.STRING, type);
    }
}
