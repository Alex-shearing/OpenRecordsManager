package com.openrecordsmanager.plugin.defaults_aus_gov;

import com.openrecordsmanager.api.template.list.IListElement;
import com.openrecordsmanager.api.template.property.ObjectPropertyTemplate;
import com.openrecordsmanager.api.template.property.PropertyType;

import java.util.Collection;

public class UserProperties {
    public static final ObjectPropertyTemplate<IListElement> USER_SECURITY_CLASSIFICATION =
            ObjectPropertyTemplate.builder("Allowed Security Classification", PropertyType.LIST_ITEM)
                    .description("The top security classification the user is able to access.")
                    .listType(Lists.SECURITY_CLASSIFICATION)
                    .build();

    public static final ObjectPropertyTemplate<Collection<IListElement>> USER_SECURITY_CAVEAT =
            ObjectPropertyTemplate.builder("Allowed Security Caveat", PropertyType.LIST_MULTIPLE)
                    .description("List of security caveats that the user is able to access.")
                    .listType(Lists.SECURITY_CAVEAT)
                    .build();
}
