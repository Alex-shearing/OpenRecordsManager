package com.openrecordsmanager.security;

import com.openrecordsmanager.list.IListElement;
import com.openrecordsmanager.model.*;
import com.openrecordsmanager.property.PropertyType;
import com.openrecordsmanager.resources.ResourceIdentifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Set;

@SpringBootTest
class RecordSecurityFilterTest {

    // Construct list
    private static final ListType LIST_TYPE = new ListType(ResourceIdentifier.valueOf("test:list"));
    private static final ListElement LIST_ITEM_1 = new ListElement(ResourceIdentifier.valueOf("test:list_element_1"), LIST_TYPE, "List Element 1", "", 1, null, Set.of());
    private static final ListElement LIST_ITEM_2 = new ListElement(ResourceIdentifier.valueOf("test:list_element_2"), LIST_TYPE, "List Element 2", "", 2, null, Set.of());
    private static final ListElement LIST_ITEM_3 = new ListElement(ResourceIdentifier.valueOf("test:list_element_3"), LIST_TYPE, "List Element 3", "", 3, null, Set.of());

    static {
        LIST_TYPE.children.addAll(Set.of(LIST_ITEM_1, LIST_ITEM_2, LIST_ITEM_3));
    }


    @Test
    void securityFilter_stringValueEquals() {
        // Construct user property
        ObjectProperty<String> usrProperty = new ObjectProperty<>(ResourceIdentifier.valueOf("test:user_property"));
        usrProperty.type = PropertyType.STRING;

        // Construct record property
        ObjectProperty<String> recProperty = new ObjectProperty<>(ResourceIdentifier.valueOf("test:record_property"));
        recProperty.type = PropertyType.STRING;
        recProperty.securityFilter = "value == user['test:user_property']";

        RecordPropertyValue<String> value = new RecordPropertyValue<>(null, recProperty, "test value");

        User user = new User();
        user.properties.add(new UserPropertyValue<>(user, usrProperty, "test value"));

        Assertions.assertTrue(RecordSecurityFilter.securityFilter(value, user), "User should have access");
    }

    @Test
    void securityFilter_listItem() {
        // Construct user property
        ObjectProperty<IListElement> usrProperty = new ObjectProperty<>(ResourceIdentifier.valueOf("test:user_property"));
        usrProperty.type = PropertyType.LIST_ITEM;
        usrProperty.listType = LIST_TYPE;

        // Construct user
        User user = new User();
        user.properties.add(new UserPropertyValue<>(user, usrProperty, LIST_ITEM_2));

        // Construct record property
        ObjectProperty<IListElement> recProperty = new ObjectProperty<>(ResourceIdentifier.valueOf("test:record_property"));
        recProperty.type = PropertyType.LIST_ITEM;
        recProperty.listType = LIST_TYPE;
        recProperty.securityFilter = "value == user['test:user_property']";

        ObjectProperty<IListElement> recPropertyIndex = new ObjectProperty<>(ResourceIdentifier.valueOf("test:record_property"));
        recPropertyIndex.type = PropertyType.LIST_ITEM;
        recPropertyIndex.listType = LIST_TYPE;
        recPropertyIndex.securityFilter = "user['test:user_property'] >= value";

        RecordPropertyValue<IListElement> value = new RecordPropertyValue<>(null, recProperty, LIST_ITEM_2);
        RecordPropertyValue<IListElement> value2 = new RecordPropertyValue<>(null, recProperty, LIST_ITEM_1);
        RecordPropertyValue<IListElement> indexValue = new RecordPropertyValue<>(null, recPropertyIndex, LIST_ITEM_2);

        Assertions.assertTrue(RecordSecurityFilter.securityFilter(value, user), "User should have access");
        Assertions.assertFalse(RecordSecurityFilter.securityFilter(value2, user), "User should not have access");
        Assertions.assertTrue(RecordSecurityFilter.securityFilter(indexValue, user), "User should have access");
    }

    @Test
    void securityFilter_listItemList() {
        // Construct user property
        ObjectProperty<List<IListElement>> usrProperty = new ObjectProperty<>(ResourceIdentifier.valueOf("test:user_property"));
        usrProperty.type = PropertyType.LIST_MULTIPLE;
        usrProperty.listType = LIST_TYPE;

        // Construct user
        User user = new User();
        user.properties.add(new UserPropertyValue<>(user, usrProperty, List.of(LIST_ITEM_1, LIST_ITEM_3)));

        // Construct record property
        ObjectProperty<IListElement> recProperty = new ObjectProperty<>(ResourceIdentifier.valueOf("test:record_property"));
        recProperty.type = PropertyType.LIST_ITEM;
        recProperty.listType = LIST_TYPE;
        recProperty.securityFilter = "value in user['test:user_property']";

        RecordPropertyValue<IListElement> value = new RecordPropertyValue<>(null, recProperty, LIST_ITEM_1);
        RecordPropertyValue<IListElement> value2 = new RecordPropertyValue<>(null, recProperty, LIST_ITEM_2);

        Assertions.assertTrue(RecordSecurityFilter.securityFilter(value, user), "User should have access");
        Assertions.assertFalse(RecordSecurityFilter.securityFilter(value2, user), "User should not have access");
    }

    @Test
    void securityFilter_listListItemList() {
        // Construct user property
        ObjectProperty<List<IListElement>> usrProperty = new ObjectProperty<>(ResourceIdentifier.valueOf("test:user_property"));
        usrProperty.type = PropertyType.LIST_MULTIPLE;
        usrProperty.listType = LIST_TYPE;

        // Construct users
        User allowedUser = new User();
        allowedUser.properties.add(new UserPropertyValue<>(allowedUser, usrProperty, List.of(LIST_ITEM_1, LIST_ITEM_2, LIST_ITEM_3)));
        User disallowedUser = new User();
        disallowedUser.properties.add(new UserPropertyValue<>(allowedUser, usrProperty, List.of(LIST_ITEM_1)));

        // Construct record property
        ObjectProperty<List<IListElement>> recProperty = new ObjectProperty<>(ResourceIdentifier.valueOf("test:record_property"));
        recProperty.type = PropertyType.LIST_MULTIPLE;
        recProperty.listType = LIST_TYPE;
        recProperty.securityFilter = "value.all(x, x in user['test:user_property'])";

        RecordPropertyValue<List<IListElement>> value = new RecordPropertyValue<>(null, recProperty, List.of(LIST_ITEM_1, LIST_ITEM_3));

        Assertions.assertTrue(RecordSecurityFilter.securityFilter(value, allowedUser), "User should have access");
        Assertions.assertFalse(RecordSecurityFilter.securityFilter(value, disallowedUser), "User should not have access");
    }
}