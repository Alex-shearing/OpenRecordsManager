package com.openrecordsmanager.plugin;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.list.IListElement;
import com.openrecordsmanager.api.template.property.PropertyType;
import com.openrecordsmanager.api.template.recordtype.SecurityFilterUsage;
import com.openrecordsmanager.list.ListElement;
import com.openrecordsmanager.list.ListType;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.record.Record;
import com.openrecordsmanager.recordtype.RecordType;
import com.openrecordsmanager.recordtype.RecordTypeProperty;
import com.openrecordsmanager.user.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

@SpringBootTest
class ExpressionsServiceTest {

    private static final UUID EMPTY_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    // Construct list
    private static final ListType LIST_TYPE = new ListType(ResourceIdentifier.valueOf("test:list"), "List");
    private static final ListElement LIST_ITEM_1 = new ListElement(ResourceIdentifier.valueOf("test:list_element_1"), LIST_TYPE, "List Element 1", "", 1, null, Set.of());
    private static final ListElement LIST_ITEM_2 = new ListElement(ResourceIdentifier.valueOf("test:list_element_2"), LIST_TYPE, "List Element 2", "", 2, null, Set.of());
    private static final ListElement LIST_ITEM_3 = new ListElement(ResourceIdentifier.valueOf("test:list_element_3"), LIST_TYPE, "List Element 3", "", 3, null, Set.of());


    @BeforeAll
    static void setupList() {
        LIST_TYPE.children.addAll(Set.of(LIST_ITEM_1, LIST_ITEM_2, LIST_ITEM_3));
    }

    // Construct test user
    private User testUser;

    @BeforeEach
    void setUp() {
        this.testUser = new User(UUID.randomUUID(), "test");

        // Number property
        ObjectProperty<Long> numberProperty = new ObjectProperty<>(
                com.openrecordsmanager.api.ResourceIdentifier.valueOf("test:number_property"),
                "Number property", "Number property", PropertyType.NUMBER
        );
        this.testUser.setProperty(numberProperty, 10L);

        // String property
        ObjectProperty<String> stringProperty = new ObjectProperty<>(
                com.openrecordsmanager.api.ResourceIdentifier.valueOf("test:string_property"),
                "String property", "String property", PropertyType.STRING
        );
        this.testUser.setProperty(stringProperty, "test value");

        // List property
        ObjectProperty<IListElement> listProperty = new ObjectProperty<>(
                com.openrecordsmanager.api.ResourceIdentifier.valueOf("test:list_property"),
                "List property", "List property", PropertyType.LIST_ITEM
        );
        this.testUser.setProperty(listProperty, LIST_ITEM_2);

        // List multiple property
        ObjectProperty<Collection<IListElement>> listMultiple = new ObjectProperty<>(
                com.openrecordsmanager.api.ResourceIdentifier.valueOf("test:list_multiple_property"),
                "List multiple property", "List multiple property", PropertyType.LIST_MULTIPLE
        );
        this.testUser.setProperty(listMultiple, List.of(LIST_ITEM_1, LIST_ITEM_3));
    }

    @Autowired
    private ExpressionsService expressions;

    @Test
    void checkPropertyExpression_numberValue() {
        // Check user and record comparisons
        Assertions.assertTrue(this.expressions.checkPropertyExpression(EMPTY_UUID, "principal['test:number_property'] == value", 10, this.testUser, null), "Number equals: User should have access");
        Assertions.assertTrue(this.expressions.checkPropertyExpression(EMPTY_UUID, "principal['test:number_property'] >= value", 5, this.testUser, null), "Number greater than: User should have access");
        Assertions.assertFalse(this.expressions.checkPropertyExpression(EMPTY_UUID, "principal['test:number_property'] > value", 15, this.testUser, null), "Number greater than: User should not have access");
    }

    @Test
    void checkPropertyExpression_stringValue() {
        // Check user and record have the same string value
        Assertions.assertTrue(this.expressions.checkPropertyExpression(EMPTY_UUID, "value == principal['test:string_property']", "test value", this.testUser, null), "String equals: User should have access");
        Assertions.assertFalse(this.expressions.checkPropertyExpression(EMPTY_UUID, "value == principal['test:string_property']", "other value", this.testUser, null), "String equals: User should not have access");
        Assertions.assertTrue(this.expressions.checkPropertyExpression(EMPTY_UUID, "value != principal['test:string_property']", "other value", this.testUser, null), "String does not equal: User should have access");
    }

    @Test
    void checkPropertyExpression_listItem() {
        // Check user and record list value match
        Assertions.assertTrue(this.expressions.checkPropertyExpression(EMPTY_UUID, "value == principal['test:list_property']", LIST_ITEM_2, this.testUser, null), "List item equals: User should have access");
        Assertions.assertFalse(this.expressions.checkPropertyExpression(EMPTY_UUID, "value == principal['test:list_property']", LIST_ITEM_1, this.testUser, null), "List item equals: User should not have access");
        Assertions.assertTrue(this.expressions.checkPropertyExpression(EMPTY_UUID, "value != principal['test:list_property']", LIST_ITEM_1, this.testUser, null), "List item not equals: User should have access");

        // Check user has a higher index than the record list property
        Assertions.assertTrue(this.expressions.checkPropertyExpression(EMPTY_UUID, "principal['test:list_property'] >= value", LIST_ITEM_2, this.testUser, null), "List index greater than: User should have access");
    }

    @Test
    void checkPropertyExpression_listItemList() {
        // Check user has the records property value in a list property of the user
        Assertions.assertTrue(this.expressions.checkPropertyExpression(EMPTY_UUID, "value in principal['test:list_multiple_property']", LIST_ITEM_1, this.testUser, null), "Value in list: User should have access");
        Assertions.assertFalse(this.expressions.checkPropertyExpression(EMPTY_UUID, "value in principal['test:list_multiple_property']", LIST_ITEM_2, this.testUser, null), "Value in list: User should not have access");
    }

    @Test
    void checkPropertyExpression_listListItemList() {
        // Check user has all required items
        Assertions.assertTrue(this.expressions.checkPropertyExpression(EMPTY_UUID, "value.all(x, x in principal['test:list_multiple_property'])", List.of(LIST_ITEM_1, LIST_ITEM_3), this.testUser, null), "All values in list: User should have access");
        Assertions.assertFalse(this.expressions.checkPropertyExpression(EMPTY_UUID, "value.all(x, x in principal['test:list_multiple_property'])", List.of(LIST_ITEM_1, LIST_ITEM_2, LIST_ITEM_3), this.testUser, null), "All values in list: User should not have access");
    }

    @Test
    void checkPropertyExpression_withRecord() {
        ObjectProperty<Boolean> recordProperty = new ObjectProperty<>(ResourceIdentifier.valueOf("test:record_boolean"), "Record Boolean", "Record Boolean", PropertyType.BOOLEAN);

        RecordType recordType = new RecordType(
                ResourceIdentifier.valueOf("test:record_type"),
                "Record type",
                "Record type",
                null,
                null,
                SecurityFilterUsage.HIDE_RECORD,
                new HashSet<>()
        );
        recordType.properties.add(new RecordTypeProperty<>(recordProperty, false));
        
        Record record = new Record("Record title", recordType);
        record.setProperty(recordProperty, false);

        // Check user has all required items
        Assertions.assertTrue(this.expressions.checkPropertyExpression(EMPTY_UUID, "value == principal['test:string_property'] && resource['test:record_boolean'] == false", "test value", this.testUser, record), "Extended resource: User should have access");
        Assertions.assertFalse(this.expressions.checkPropertyExpression(EMPTY_UUID, "value == principal['test:string_property'] && resource['test:record_boolean']", "test value", this.testUser, record), "Extended resource: User should not have access");
    }
}