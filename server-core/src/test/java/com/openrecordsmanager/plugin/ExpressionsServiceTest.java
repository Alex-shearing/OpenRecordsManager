package com.openrecordsmanager.plugin;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.api.location.LocationKind;
import com.openrecordsmanager.api.template.list.IListElement;
import com.openrecordsmanager.api.template.property.PropertyType;
import com.openrecordsmanager.api.template.recordtype.SecurityFilterUsage;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.database.SqliteTestSupport;
import com.openrecordsmanager.list.ListElement;
import com.openrecordsmanager.list.ListType;
import com.openrecordsmanager.location.type.LocationType;
import com.openrecordsmanager.location.type.LocationTypeProperty;
import com.openrecordsmanager.location.user.User;
import com.openrecordsmanager.property.ObjectProperty;
import com.openrecordsmanager.record.Record;
import com.openrecordsmanager.record.type.RecordType;
import com.openrecordsmanager.record.type.RecordTypeProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.node.JsonNodeFactory;

import java.util.*;

@SpringBootTest
class ExpressionsServiceTest {

    private static final UUID EMPTY_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final ResourceIdentifier LIST_ID = ResourceIdentifier.valueOf("test:expr_list");
    private static final ResourceIdentifier ELEMENT_1 = ResourceIdentifier.valueOf("test:expr_list_element_1");
    private static final ResourceIdentifier ELEMENT_2 = ResourceIdentifier.valueOf("test:expr_list_element_2");
    private static final ResourceIdentifier ELEMENT_3 = ResourceIdentifier.valueOf("test:expr_list_element_3");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SqliteTestSupport.registerPrimaryMemoryDatabase(registry, ExpressionsServiceTest.class);
        registry.add(BuiltinConfigs.PLUGINS_SKIP_SYNC.key(), () -> "true");
        registry.add(BuiltinConfigs.COOKIE_SECURE.key(), () -> "false");
    }

    @Autowired
    private ExpressionsService expressions;

    @Autowired
    private DataRepository repository;

    private User testUser;
    private ListElement listItem1;
    private ListElement listItem2;
    private ListElement listItem3;

    @BeforeEach
    void setUp() {
        ListType listType = this.repository.listTypeRepo.findById(LIST_ID).orElseGet(() ->
                this.repository.listTypeRepo.saveAndFlush(new ListType(LIST_ID))
        );

        this.listItem1 = this.repository.listElementRepo.findById(ELEMENT_1).orElseGet(() ->
                this.repository.listElementRepo.saveAndFlush(
                        new ListElement(ELEMENT_1, listType, 1, null, Set.of())
                )
        );
        this.listItem2 = this.repository.listElementRepo.findById(ELEMENT_2).orElseGet(() ->
                this.repository.listElementRepo.saveAndFlush(
                        new ListElement(ELEMENT_2, listType, 2, null, Set.of())
                )
        );
        this.listItem3 = this.repository.listElementRepo.findById(ELEMENT_3).orElseGet(() ->
                this.repository.listElementRepo.saveAndFlush(
                        new ListElement(ELEMENT_3, listType, 3, null, Set.of())
                )
        );

        ObjectProperty<Long> numberProperty = new ObjectProperty<>(
                ResourceIdentifier.valueOf("test:number_property"),
                PropertyType.NUMBER
        );
        ObjectProperty<String> stringProperty = new ObjectProperty<>(
                ResourceIdentifier.valueOf("test:string_property"),
                PropertyType.STRING
        );
        ObjectProperty<IListElement> listProperty = new ObjectProperty<>(
                ResourceIdentifier.valueOf("test:list_property"),
                PropertyType.LIST_ITEM,
                listType,
                null,
                null,
                null,
                false
        );
        ObjectProperty<Collection<IListElement>> listMultiple = new ObjectProperty<>(
                ResourceIdentifier.valueOf("test:list_multiple_property"),
                PropertyType.LIST_MULTIPLE,
                listType,
                null,
                null,
                null,
                false
        );
        LocationType userType = new LocationType(
                ResourceIdentifier.valueOf("test:expr_user_type"),
                LocationKind.USER,
                Set.of(
                        new LocationTypeProperty<>(numberProperty, null),
                        new LocationTypeProperty<>(stringProperty, null),
                        new LocationTypeProperty<>(listProperty, null),
                        new LocationTypeProperty<>(listMultiple, null)
                )
        );
        this.testUser = new User("test", null, userType);
        this.testUser.setProperty(numberProperty, 10L);
        this.testUser.setProperty(stringProperty, "test value");
        this.testUser.setProperty(listProperty, this.listItem2);
        this.testUser.setProperty(listMultiple, List.of(this.listItem1, this.listItem3));
    }

    @Test
    void checkPropertyExpression_numberValue() {
        Assertions.assertTrue(this.expressions.checkPropertyExpression(EMPTY_UUID, "principal['test:number_property'] == value", 10, this.testUser, null), "Number equals: User should have access");
        Assertions.assertTrue(this.expressions.checkPropertyExpression(EMPTY_UUID, "principal['test:number_property'] >= value", 5, this.testUser, null), "Number greater than: User should have access");
        Assertions.assertFalse(this.expressions.checkPropertyExpression(EMPTY_UUID, "principal['test:number_property'] > value", 15, this.testUser, null), "Number greater than: User should not have access");
    }

    @Test
    void checkPropertyExpression_stringValue() {
        Assertions.assertTrue(this.expressions.checkPropertyExpression(EMPTY_UUID, "value == principal['test:string_property']", "test value", this.testUser, null), "String equals: User should have access");
        Assertions.assertFalse(this.expressions.checkPropertyExpression(EMPTY_UUID, "value == principal['test:string_property']", "other value", this.testUser, null), "String equals: User should not have access");
        Assertions.assertTrue(this.expressions.checkPropertyExpression(EMPTY_UUID, "value != principal['test:string_property']", "other value", this.testUser, null), "String does not equal: User should have access");
    }

    @Test
    void checkPropertyExpression_listItem() {
        Assertions.assertTrue(this.expressions.checkPropertyExpression(EMPTY_UUID, "value == principal['test:list_property']", this.listItem2, this.testUser, null), "List item equals: User should have access");
        Assertions.assertFalse(this.expressions.checkPropertyExpression(EMPTY_UUID, "value == principal['test:list_property']", this.listItem1, this.testUser, null), "List item equals: User should not have access");
        Assertions.assertTrue(this.expressions.checkPropertyExpression(EMPTY_UUID, "value != principal['test:list_property']", this.listItem1, this.testUser, null), "List item not equals: User should have access");
        Assertions.assertTrue(this.expressions.checkPropertyExpression(EMPTY_UUID, "principal['test:list_property'] >= value", this.listItem2, this.testUser, null), "List index greater than: User should have access");
    }

    @Test
    void checkPropertyExpression_listItemList() {
        Assertions.assertTrue(this.expressions.checkPropertyExpression(EMPTY_UUID, "value in principal['test:list_multiple_property']", this.listItem1, this.testUser, null), "Value in list: User should have access");
        Assertions.assertFalse(this.expressions.checkPropertyExpression(EMPTY_UUID, "value in principal['test:list_multiple_property']", this.listItem2, this.testUser, null), "Value in list: User should not have access");
    }

    @Test
    void checkPropertyExpression_listListItemList() {
        Assertions.assertTrue(this.expressions.checkPropertyExpression(EMPTY_UUID, "value.all(x, x in principal['test:list_multiple_property'])", List.of(this.listItem1, this.listItem3), this.testUser, null), "All values in list: User should have access");
        Assertions.assertFalse(this.expressions.checkPropertyExpression(EMPTY_UUID, "value.all(x, x in principal['test:list_multiple_property'])", List.of(this.listItem1, this.listItem2, this.listItem3), this.testUser, null), "All values in list: User should not have access");
    }

    @Test
    void checkPropertyExpression_withRecord() {
        ObjectProperty<Boolean> recordProperty = new ObjectProperty<>(
                ResourceIdentifier.valueOf("test:record_boolean"),
                PropertyType.BOOLEAN
        );

        RecordType recordType = new RecordType(
                ResourceIdentifier.valueOf("test:record_type"),
                null,
                null,
                SecurityFilterUsage.HIDE_RECORD,
                new HashSet<>()
        );
        recordType.getProperties().add(new RecordTypeProperty<>(
                recordProperty,
                JsonNodeFactory.instance.booleanNode(false)
        ));

        Record record = new Record("Record title", recordType);
        record.setProperty(recordProperty, false);

        Assertions.assertTrue(this.expressions.checkPropertyExpression(EMPTY_UUID, "value == principal['test:string_property'] && resource['test:record_boolean'] == false", "test value", this.testUser, record), "Extended resource: User should have access");
        Assertions.assertFalse(this.expressions.checkPropertyExpression(EMPTY_UUID, "value == principal['test:string_property'] && resource['test:record_boolean']", "test value", this.testUser, record), "Extended resource: User should not have access");
    }
}
