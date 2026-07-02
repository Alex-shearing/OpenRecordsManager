package com.openrecordsmanager.plugin.defaults_aus_gov;

import com.openrecordsmanager.api.list.IListElement;
import com.openrecordsmanager.api.property.PropertyDefinition;
import com.openrecordsmanager.api.property.PropertyType;

import java.util.Collection;

public class UserProperties {
    public static final PropertyDefinition<IListElement> USER_SECURITY_CLASSIFICATION = PropertyDefinition.builder("user_security_classification", PropertyType.LIST_ITEM)
            .name("Allowed Security Classification")
            .description("The top security classification the user is able to access.")
            .listType(Lists.SECURITY_CLASSIFICATION)
            .build();
    public static final PropertyDefinition<Collection<IListElement>> USER_SECURITY_CAVEAT = PropertyDefinition.builder("user_security_caveat", PropertyType.LIST_MULTIPLE)
            .name("Allowed Security Caveat")
            .description("List of security caveats that the user is able to access.")
            .listType(Lists.SECURITY_CAVEAT)
            .build();
}
