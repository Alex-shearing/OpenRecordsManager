package com.openrecordsmanager.property;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.errors.InputValidationException;
import com.openrecordsmanager.api.template.list.IListElement;
import com.openrecordsmanager.api.template.property.PropertyType;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.database.SqliteTestSupport;
import com.openrecordsmanager.list.ListElement;
import com.openrecordsmanager.list.ListType;
import com.openrecordsmanager.rest.errors.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PropertyValueCodecTest {

    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;
    private static final ResourceIdentifier LIST_ID = ResourceIdentifier.valueOf("test:resolver_list");
    private static final ResourceIdentifier ELEMENT_A = ResourceIdentifier.valueOf("test:resolver_a");
    private static final ResourceIdentifier ELEMENT_B = ResourceIdentifier.valueOf("test:resolver_b");
    private static final ResourceIdentifier OTHER_LIST = ResourceIdentifier.valueOf("test:resolver_other_list");
    private static final ResourceIdentifier OTHER_ELEMENT = ResourceIdentifier.valueOf("test:resolver_other");
    private static final ResourceIdentifier ITEM_PROP = ResourceIdentifier.valueOf("test:resolver_list_item");
    private static final ResourceIdentifier MULTI_PROP = ResourceIdentifier.valueOf("test:resolver_list_multiple");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SqliteTestSupport.registerPrimaryMemoryDatabase(registry, PropertyValueCodecTest.class);
        registry.add(BuiltinConfigs.PLUGINS_SKIP_SYNC.key(), () -> "true");
        registry.add(BuiltinConfigs.COOKIE_SECURE.key(), () -> "false");
    }

    @Autowired
    private PropertyValueCodec codec;

    @Autowired
    private DataRepository repository;

    private ObjectProperty<IListElement> listItemProperty;
    private ObjectProperty<Collection<IListElement>> listMultipleProperty;
    private ListElement elementA;
    private ListElement elementB;

    @BeforeEach
    void setUp() {
        ListType listType = this.repository.listTypeRepo.findById(LIST_ID).orElseGet(() ->
                this.repository.listTypeRepo.saveAndFlush(new ListType(LIST_ID, "Resolver list"))
        );

        ListType otherList = this.repository.listTypeRepo.findById(OTHER_LIST).orElseGet(() ->
                this.repository.listTypeRepo.saveAndFlush(new ListType(OTHER_LIST, "Other list"))
        );

        this.elementA = this.repository.listElementRepo.findById(ELEMENT_A).orElseGet(() ->
                this.repository.listElementRepo.saveAndFlush(
                        new ListElement(ELEMENT_A, listType, "A", "", 1, null, Set.of())
                )
        );
        this.elementB = this.repository.listElementRepo.findById(ELEMENT_B).orElseGet(() ->
                this.repository.listElementRepo.saveAndFlush(
                        new ListElement(ELEMENT_B, listType, "B", "", 2, null, Set.of())
                )
        );
        this.repository.listElementRepo.findById(OTHER_ELEMENT).orElseGet(() ->
                this.repository.listElementRepo.saveAndFlush(
                        new ListElement(OTHER_ELEMENT, otherList, "Other", "", 1, null, Set.of())
                )
        );

        this.listItemProperty = this.repository.objectPropertyRepo.findById(ITEM_PROP)
                .map(p -> (ObjectProperty<IListElement>) p)
                .orElseGet(() -> this.repository.objectPropertyRepo.saveAndFlush(new ObjectProperty<>(
                        ITEM_PROP,
                        "List item",
                        "List item",
                        PropertyType.LIST_ITEM,
                        listType,
                        null,
                        null,
                        null,
                        false
                )));

        this.listMultipleProperty = this.repository.objectPropertyRepo.findById(MULTI_PROP)
                .map(p -> (ObjectProperty<Collection<IListElement>>) p)
                .orElseGet(() -> this.repository.objectPropertyRepo.saveAndFlush(new ObjectProperty<>(
                        MULTI_PROP,
                        "List multiple",
                        "List multiple",
                        PropertyType.LIST_MULTIPLE,
                        listType,
                        null,
                        null,
                        null,
                        false
                )));
    }

    @Test
    void parseListItemFromString() {
        Object resolved = this.codec.parse(this.listItemProperty, NODES.textNode(ELEMENT_A.toString()));
        assertEquals(this.elementA, resolved);
    }

    @Test
    void parseListMultipleFromStrings() {
        Object resolved = this.codec.parse(
                this.listMultipleProperty,
                NODES.arrayNode().add(ELEMENT_A.toString()).add(ELEMENT_B.toString())
        );
        assertEquals(List.of(this.elementA, this.elementB), resolved);
    }

    @Test
    void parseUnknownIdThrows() {
        assertThrows(
                ResourceNotFoundException.class,
                () -> this.codec.parse(this.listItemProperty, NODES.textNode("test:missing_element"))
        );
    }

    @Test
    void parseWrongListThrows() {
        assertThrows(
                ResourceNotFoundException.class,
                () -> this.codec.parse(this.listItemProperty, NODES.textNode(OTHER_ELEMENT.toString()))
        );
    }

    @Test
    void parseInvalidCollectionThrows() {
        assertThrows(
                InputValidationException.class,
                () -> this.codec.parse(this.listMultipleProperty, NODES.textNode(ELEMENT_A.toString()))
        );
    }

    @Test
    void toStoredAndHydrateRoundTrip() {
        JsonNode stored = this.codec.toStored(this.listItemProperty, this.elementA);
        assertEquals(ELEMENT_A.toString(), stored.asString());

        Object hydrated = this.codec.hydrate(this.listItemProperty, stored);
        assertEquals(this.elementA, hydrated);

        JsonNode multiStored = this.codec.toStored(
                this.listMultipleProperty,
                List.of(this.elementA, this.elementB)
        );
        assertTrue(multiStored.isArray());
        assertEquals(ELEMENT_A.toString(), multiStored.get(0).asString());
        assertEquals(ELEMENT_B.toString(), multiStored.get(1).asString());

        Object multiHydrated = this.codec.hydrate(this.listMultipleProperty, multiStored);
        assertEquals(List.of(this.elementA, this.elementB), multiHydrated);
    }

    @Test
    void encodeListItemAsIdString() {
        assertEquals(ELEMENT_A.toString(), this.codec.encode(this.listItemProperty, this.elementA).asString());
    }
}
