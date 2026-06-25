package com.openrecordsmanager.resources;

import com.openrecordsmanager.api.list.IListElement;
import com.openrecordsmanager.api.property.PropertyType;
import com.openrecordsmanager.model.*;
import com.openrecordsmanager.model.Record;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@SpringBootTest
class ExpressionsServiceTest {

    private static final UUID EMPTY_UUID = UUID.fromString("00000000-0000-0000-0000-00000000000");

    // Construct list
    private static final ListType LIST_TYPE = new ListType(ResourceIdentifier.valueOf("test:list"), "List");
    private static final ListElement LIST_ITEM_1 = new ListElement(ResourceIdentifier.valueOf("test:list_element_1"), LIST_TYPE, "List Element 1", "", 1, null, Set.of());
    private static final ListElement LIST_ITEM_2 = new ListElement(ResourceIdentifier.valueOf("test:list_element_2"), LIST_TYPE, "List Element 2", "", 2, null, Set.of());
    private static final ListElement LIST_ITEM_3 = new ListElement(ResourceIdentifier.valueOf("test:list_element_3"), LIST_TYPE, "List Element 3", "", 3, null, Set.of());

    // Construct test user
    private static final User TEST_USER = new User(UUID.randomUUID());

    static {
        // Add all list children
        LIST_TYPE.children.addAll(Set.of(LIST_ITEM_1, LIST_ITEM_2, LIST_ITEM_3));

        // Number property
        ObjectProperty<Long> numberProperty = new ObjectProperty<>(
                ResourceIdentifier.valueOf("test:number_property"),
                "Number property",
                "Number property",
                PropertyType.NUMBER
        );
        TEST_USER.setProperty(numberProperty, 10L);

        // String property
        ObjectProperty<String> stringProperty = new ObjectProperty<>(
                ResourceIdentifier.valueOf("test:string_property"),
                "String property",
                "String property",
                PropertyType.STRING
        );
        TEST_USER.setProperty(stringProperty, "test value");

        // List property
        ObjectProperty<IListElement> listProperty = new ObjectProperty<>(
                ResourceIdentifier.valueOf("test:list_property"),
                "List property",
                "List property",
                PropertyType.LIST_ITEM
        );
        TEST_USER.setProperty(listProperty, LIST_ITEM_2);

        // List multiple property
        ObjectProperty<List<IListElement>> listMultiple = new ObjectProperty<>(
                ResourceIdentifier.valueOf("test:list_multiple_property"),
                "List multiple property",
                "List multiple property",
                PropertyType.LIST_MULTIPLE
        );
        TEST_USER.setProperty(listMultiple, List.of(LIST_ITEM_1, LIST_ITEM_3));
    }

    @Autowired
    private ExpressionsService expressions;

    @Test
    void checkPropertyExpression_numberValue() {
        // Check user and record comparisons
        Assertions.assertTrue(this.expressions.checkPropertyExpression(EMPTY_UUID, "principal['test:number_property'] == value", 10, TEST_USER, null), "Number equals: User should have access");
        Assertions.assertTrue(this.expressions.checkPropertyExpression(EMPTY_UUID, "principal['test:number_property'] >= value", 5, TEST_USER, null), "Number greater than: User should have access");
        Assertions.assertFalse(this.expressions.checkPropertyExpression(EMPTY_UUID, "principal['test:number_property'] > value", 15, TEST_USER, null), "Number greater than: User should not have access");
    }

    @Test
    void checkPropertyExpression_stringValue() {
        // Check user and record have the same string value
        Assertions.assertTrue(this.expressions.checkPropertyExpression(EMPTY_UUID, "value == principal['test:string_property']", "test value", TEST_USER, null), "String equals: User should have access");
        Assertions.assertFalse(this.expressions.checkPropertyExpression(EMPTY_UUID, "value == principal['test:string_property']", "other value", TEST_USER, null), "String equals: User should not have access");
        Assertions.assertTrue(this.expressions.checkPropertyExpression(EMPTY_UUID, "value != principal['test:string_property']", "other value", TEST_USER, null), "String does not equal: User should have access");
    }

    @Test
    void checkPropertyExpression_listItem() {
        // Check user and record list value match
        Assertions.assertTrue(this.expressions.checkPropertyExpression(EMPTY_UUID, "value == principal['test:list_property']", LIST_ITEM_2, TEST_USER, null), "List item equals: User should have access");
        Assertions.assertFalse(this.expressions.checkPropertyExpression(EMPTY_UUID, "value == principal['test:list_property']", LIST_ITEM_1, TEST_USER, null), "List item equals: User should not have access");
        Assertions.assertTrue(this.expressions.checkPropertyExpression(EMPTY_UUID, "value != principal['test:list_property']", LIST_ITEM_1, TEST_USER, null), "List item not equals: User should have access");

        // Check user has a higher index than the record list property
        Assertions.assertTrue(this.expressions.checkPropertyExpression(EMPTY_UUID, "principal['test:list_property'] >= value", LIST_ITEM_2, TEST_USER, null), "List index greater than: User should have access");
    }

    @Test
    void checkPropertyExpression_listItemList() {
        // Check user has the records property value in a list property of the user
        Assertions.assertTrue(this.expressions.checkPropertyExpression(EMPTY_UUID, "value in principal['test:list_multiple_property']", LIST_ITEM_1, TEST_USER, null), "Value in list: User should have access");
        Assertions.assertFalse(this.expressions.checkPropertyExpression(EMPTY_UUID, "value in principal['test:list_multiple_property']", LIST_ITEM_2, TEST_USER, null), "Value in list: User should not have access");
    }

    @Test
    void checkPropertyExpression_listListItemList() {
        // Check user has all required items
        Assertions.assertTrue(this.expressions.checkPropertyExpression(EMPTY_UUID, "value.all(x, x in principal['test:list_multiple_property'])", List.of(LIST_ITEM_1, LIST_ITEM_3), TEST_USER, null), "All values in list: User should have access");
        Assertions.assertFalse(this.expressions.checkPropertyExpression(EMPTY_UUID, "value.all(x, x in principal['test:list_multiple_property'])", List.of(LIST_ITEM_1, LIST_ITEM_2, LIST_ITEM_3), TEST_USER, null), "All values in list: User should not have access");
    }

    @Test
    void checkPropertyExpression_withRecord() {
        ObjectProperty<Boolean> recordProperty = new ObjectProperty<>(ResourceIdentifier.valueOf("test:record_boolean"), "Record Boolean", "Record Boolean", PropertyType.BOOLEAN);

        Record record = new Record(UUID.randomUUID(), "Record title", null, null);
        record.setProperty(recordProperty, false);

        // Check user has all required items
        Assertions.assertTrue(this.expressions.checkPropertyExpression(EMPTY_UUID, "value == principal['test:string_property'] && resource['test:record_boolean'] == false", "test value", TEST_USER, record), "Extended resource: User should have access");
        Assertions.assertFalse(this.expressions.checkPropertyExpression(EMPTY_UUID, "value == principal['test:string_property'] && resource['test:record_boolean']", "test value", TEST_USER, record), "Extended resource: User should not have access");
    }
}