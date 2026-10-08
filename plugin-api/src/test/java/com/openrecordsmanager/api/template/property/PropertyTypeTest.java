package com.openrecordsmanager.api.template.property;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.list.CatalogAccess;
import com.openrecordsmanager.api.template.list.IListElement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.JsonNodeFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class PropertyTypeTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();
    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

    private static final ResourceIdentifier ELEMENT_A = ResourceIdentifier.valueOf("test:a");
    private static final ResourceIdentifier ELEMENT_B = ResourceIdentifier.valueOf("test:b");
    private static final IListElement ITEM_A = stubElement(ELEMENT_A, 0);
    private static final IListElement ITEM_B = stubElement(ELEMENT_B, 1);
    private static final CatalogAccess CATALOG = id -> {
        if (id.equals(ELEMENT_A)) {
            return ITEM_A;
        }
        if (id.equals(ELEMENT_B)) {
            return ITEM_B;
        }
        throw new IllegalStateException("unknown element: " + id);
    };

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
    void parseRejectsMismatchedShapes() {
        Assertions.assertNull(PropertyType.STRING.parse(NODES.numberNode(1)));
        Assertions.assertNull(PropertyType.STRING.parse(NODES.booleanNode(true)));
        Assertions.assertNull(PropertyType.STRING.parse(MAPPER.valueToTree(Map.of("a", 1))));
        Assertions.assertNull(PropertyType.BOOLEAN.parse(NODES.stringNode("yes")));
        Assertions.assertNull(PropertyType.BOOLEAN.parse(NODES.numberNode(1)));
        Assertions.assertNull(PropertyType.NUMBER.parse(NODES.numberNode(1.5)));
        Assertions.assertNull(PropertyType.NUMBER.parse(NODES.booleanNode(true)));
        Assertions.assertNull(PropertyType.STRING_LIST.parse(NODES.numberNode(42)));
        Assertions.assertNull(PropertyType.LIST_ITEM.parse(NODES.stringNode("test:a")));
        Assertions.assertNull(PropertyType.LIST_MULTIPLE.parse(MAPPER.valueToTree(List.of("test:a"))));
        Assertions.assertNull(PropertyType.LIST_ITEM.parse(NODES.numberNode(1), CATALOG));
        Assertions.assertNull(PropertyType.LIST_ITEM.parse(MAPPER.valueToTree(Map.of("id", "test:a")), CATALOG));
        Assertions.assertNull(PropertyType.LIST_MULTIPLE.parse(NODES.numberNode(1), CATALOG));
    }

    @Test
    void listTypesResolveViaCatalogAccess() {
        Assertions.assertSame(
                ITEM_A,
                PropertyType.LIST_ITEM.parse(NODES.stringNode(ELEMENT_A.toString()), CATALOG)
        );
        Collection<IListElement> multi = PropertyType.LIST_MULTIPLE.parse(
                MAPPER.valueToTree(List.of(ELEMENT_A.toString(), ELEMENT_B.toString())),
                CATALOG
        );
        Assertions.assertEquals(List.of(ITEM_A, ITEM_B), multi);
        Assertions.assertEquals(
                List.of(ITEM_A, ITEM_B),
                PropertyType.LIST_MULTIPLE.parse(
                        NODES.stringNode(ELEMENT_A + ";" + ELEMENT_B),
                        CATALOG
                )
        );
        Assertions.assertEquals(
                List.of(),
                PropertyType.LIST_MULTIPLE.parse(NODES.arrayNode(), CATALOG)
        );
    }

    @Test
    void stringListAcceptsArrayOrDelimitedString() {
        Assertions.assertEquals(
                List.of("a", "b"),
                PropertyType.STRING_LIST.parse(MAPPER.valueToTree(List.of("a", "b")))
        );
        Assertions.assertEquals(
                List.of("a", "b"),
                PropertyType.STRING_LIST.parse(NODES.stringNode("a;b"))
        );
        Assertions.assertNull(PropertyType.STRING_LIST.parse(MAPPER.valueToTree(List.of(1, 2))));
    }

    @Test
    void parseListsFromJsonNode() {
        Assertions.assertEquals(
                List.of("a", "b"),
                PropertyType.STRING_LIST.parse(MAPPER.valueToTree(List.of("a", "b")))
        );
        Assertions.assertEquals(
                List.of(1L, 2L),
                PropertyType.NUMBER_LIST.parse(MAPPER.valueToTree(List.of(1, 2)))
        );
        Assertions.assertEquals(
                List.of(1L, 2L),
                PropertyType.NUMBER_LIST.parse(MAPPER.valueToTree(List.of(1L, 2L)))
        );
        Assertions.assertEquals(
                List.of(1L, 2L),
                PropertyType.NUMBER_LIST.parse(NODES.stringNode("1;2"))
        );
    }

    @Test
    void nodeFromRawStringSupportsEnvOverrides() {
        Assertions.assertEquals(
                true,
                PropertyType.BOOLEAN.parse(PropertyType.nodeFromRawString("true"))
        );
        Assertions.assertEquals(
                List.of("http://localhost:5173", "http://localhost:3000"),
                PropertyType.STRING_LIST.parse(
                        PropertyType.nodeFromRawString(
                                "[\"http://localhost:5173\",\"http://localhost:3000\"]"
                        )
                )
        );
        Assertions.assertEquals(
                "Open Records Manager",
                PropertyType.STRING.parse(PropertyType.nodeFromRawString("\"Open Records Manager\""))
        );
        Assertions.assertEquals(
                "plain",
                PropertyType.STRING.parse(PropertyType.nodeFromRawString("plain"))
        );
    }

    @Test
    void listAndDateTypesDoNotSupportConfig() {
        Assertions.assertFalse(PropertyType.LIST_ITEM.supportsConfig());
        Assertions.assertFalse(PropertyType.LIST_MULTIPLE.supportsConfig());
        Assertions.assertFalse(PropertyType.DATE.supportsConfig());
        Assertions.assertTrue(PropertyType.OBJECT.supportsConfig());
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

    @Test
    void jacksonSerialisesPropertyTypeAsName() {
        Assertions.assertEquals("\"string\"", MAPPER.writeValueAsString(PropertyType.STRING));
    }

    private static IListElement stubElement(ResourceIdentifier id, int index) {
        return new IListElement() {
            @Override
            public ResourceIdentifier getId() {
                return id;
            }

            @Override
            public int index() {
                return index;
            }
        };
    }
}
