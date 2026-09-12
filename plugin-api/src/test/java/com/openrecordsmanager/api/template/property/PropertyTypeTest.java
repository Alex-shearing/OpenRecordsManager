package com.openrecordsmanager.api.template.property;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.JsonNodeFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;

class PropertyTypeTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();
    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

    @Test
    void parseScalarsFromJsonNode() {
        Assertions.assertEquals("hello", PropertyType.STRING.parse(NODES.stringNode("hello")));
        Assertions.assertEquals(true, PropertyType.BOOLEAN.parse(NODES.booleanNode(true)));
        Assertions.assertEquals(false, PropertyType.BOOLEAN.parse(NODES.stringNode("false")));
        Assertions.assertEquals(42L, PropertyType.NUMBER.parse(NODES.numberNode(42)));
        Assertions.assertEquals(42L, PropertyType.NUMBER.parse(NODES.stringNode("42")));
        Assertions.assertEquals(1.5d, PropertyType.DECIMAL.parse(NODES.numberNode(1.5)));
        Assertions.assertEquals(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                PropertyType.UUID.parse(NODES.stringNode("00000000-0000-0000-0000-000000000001"))
        );
    }

    @Test
    void parseListsFromJsonNode() {
        Assertions.assertEquals(
                List.of("a", "b"),
                PropertyType.STRING_LIST.parse(MAPPER.valueToTree(List.of("a", "b")))
        );
        Assertions.assertEquals(
                List.of(1, 2),
                PropertyType.INT_LIST.parse(MAPPER.valueToTree(List.of(1, 2)))
        );
    }

    @Test
    void parseValueFromJsonStrings() {
        Assertions.assertEquals(true, PropertyType.BOOLEAN.parseValue("true"));
        Assertions.assertEquals(
                List.of("http://localhost:5173", "http://localhost:3000"),
                PropertyType.STRING_LIST.parseValue("[\"http://localhost:5173\",\"http://localhost:3000\"]")
        );
        Assertions.assertEquals("Open Records Manager", PropertyType.STRING.parseValue("\"Open Records Manager\""));
    }

    @Test
    void listAndDateTypesDoNotSupportConfig() {
        Assertions.assertFalse(PropertyType.LIST_ITEM.supportsConfig());
        Assertions.assertFalse(PropertyType.LIST_MULTIPLE.supportsConfig());
        Assertions.assertFalse(PropertyType.DATE.supportsConfig());
        Assertions.assertTrue(PropertyType.OBJECT.supportsConfig());
    }

    @Test
    void listMultipleDoesNotAcceptStringCollectionsWithoutResolution() {
        Assertions.assertNull(PropertyType.LIST_MULTIPLE.parseValue(List.of("a:b", "c:d")));
        Assertions.assertNull(PropertyType.LIST_ITEM.parseValue("a:b"));
    }

    @Test
    void objectIsJsonNodePassThrough() {
        JsonNode bag = MAPPER.valueToTree(Map.of("url", "jdbc:sqlite::memory:"));
        Assertions.assertEquals(bag, PropertyType.OBJECT.parse(bag));
        Assertions.assertNull(PropertyType.OBJECT.parse(null));
    }

    @Test
    void jacksonDeserialisesPropertyTypeByName() {
        PropertyType<?> type = MAPPER.readValue("\"string\"", PropertyType.class);
        Assertions.assertSame(PropertyType.STRING, type);
    }
}
