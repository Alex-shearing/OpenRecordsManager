package com.openrecordsmanager.property;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.errors.InputValidationException;
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

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ListElementPropertyResolverTest {

    private static final ResourceIdentifier LIST_ID = ResourceIdentifier.valueOf("test:resolver_list");
    private static final ResourceIdentifier ELEMENT_A = ResourceIdentifier.valueOf("test:resolver_a");
    private static final ResourceIdentifier ELEMENT_B = ResourceIdentifier.valueOf("test:resolver_b");
    private static final ResourceIdentifier OTHER_LIST = ResourceIdentifier.valueOf("test:resolver_other_list");
    private static final ResourceIdentifier OTHER_ELEMENT = ResourceIdentifier.valueOf("test:resolver_other");
    private static final ResourceIdentifier ITEM_PROP = ResourceIdentifier.valueOf("test:resolver_list_item");
    private static final ResourceIdentifier MULTI_PROP = ResourceIdentifier.valueOf("test:resolver_list_multiple");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SqliteTestSupport.registerPrimaryMemoryDatabase(registry, ListElementPropertyResolverTest.class);
        registry.add(BuiltinConfigs.PLUGINS_SKIP_SYNC.key(), () -> "true");
        registry.add(BuiltinConfigs.COOKIE_SECURE.key(), () -> "false");
    }

    @Autowired
    private ListElementPropertyResolver resolver;

    @Autowired
    private DataRepository repository;

    private ObjectProperty<com.openrecordsmanager.api.template.list.IListElement> listItemProperty;
    private ObjectProperty<java.util.Collection<com.openrecordsmanager.api.template.list.IListElement>> listMultipleProperty;
    private ListElement elementA;
    private ListElement elementB;

    @BeforeEach
    void setUp() {
        ListType listType = this.repository.listTypeRepo.findById(LIST_ID).orElseGet(() -> {
            ListType created = new ListType(LIST_ID, "Resolver list");
            return this.repository.listTypeRepo.saveAndFlush(created);
        });

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
                .map(p -> (ObjectProperty<com.openrecordsmanager.api.template.list.IListElement>) p)
                .orElseGet(() -> {
                    ObjectProperty<com.openrecordsmanager.api.template.list.IListElement> created = new ObjectProperty<>(
                            ITEM_PROP,
                            "List item",
                            "List item",
                            PropertyType.LIST_ITEM,
                            listType,
                            null,
                            null,
                            null,
                            false
                    );
                    return this.repository.objectPropertyRepo.saveAndFlush(created);
                });

        this.listMultipleProperty = this.repository.objectPropertyRepo.findById(MULTI_PROP)
                .map(p -> (ObjectProperty<java.util.Collection<com.openrecordsmanager.api.template.list.IListElement>>) p)
                .orElseGet(() -> {
                    ObjectProperty<java.util.Collection<com.openrecordsmanager.api.template.list.IListElement>> created = new ObjectProperty<>(
                            MULTI_PROP,
                            "List multiple",
                            "List multiple",
                            PropertyType.LIST_MULTIPLE,
                            listType,
                            null,
                            null,
                            null,
                            false
                    );
                    return this.repository.objectPropertyRepo.saveAndFlush(created);
                });
    }

    @Test
    void resolveInputListItemFromString() {
        Object resolved = this.resolver.resolveInput(this.listItemProperty, ELEMENT_A.toString());
        assertEquals(this.elementA, resolved);
    }

    @Test
    void resolveInputListMultipleFromStrings() {
        Object resolved = this.resolver.resolveInput(
                this.listMultipleProperty,
                List.of(ELEMENT_A.toString(), ELEMENT_B.toString())
        );
        assertEquals(List.of(this.elementA, this.elementB), resolved);
    }

    @Test
    void resolveInputUnknownIdThrows() {
        assertThrows(
                ResourceNotFoundException.class,
                () -> this.resolver.resolveInput(this.listItemProperty, "test:missing_element")
        );
    }

    @Test
    void resolveInputWrongListThrows() {
        assertThrows(
                ResourceNotFoundException.class,
                () -> this.resolver.resolveInput(this.listItemProperty, OTHER_ELEMENT.toString())
        );
    }

    @Test
    void resolveInputInvalidCollectionThrows() {
        assertThrows(
                InputValidationException.class,
                () -> this.resolver.resolveInput(this.listMultipleProperty, ELEMENT_A.toString())
        );
    }

    @Test
    void toStoredValueAndHydrateRoundTrip() {
        Object stored = this.resolver.toStoredValue(this.listItemProperty, this.elementA);
        assertEquals(ELEMENT_A.toString(), stored);

        Object hydrated = this.resolver.hydrateStored(this.listItemProperty, stored);
        assertEquals(this.elementA, hydrated);

        Object multiStored = this.resolver.toStoredValue(
                this.listMultipleProperty,
                List.of(this.elementA, this.elementB)
        );
        assertEquals(List.of(ELEMENT_A.toString(), ELEMENT_B.toString()), multiStored);

        Object multiHydrated = this.resolver.hydrateStored(this.listMultipleProperty, multiStored);
        assertEquals(List.of(this.elementA, this.elementB), multiHydrated);
    }
}
