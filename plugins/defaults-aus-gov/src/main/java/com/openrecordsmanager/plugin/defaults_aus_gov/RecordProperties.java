package com.openrecordsmanager.plugin.defaults_aus_gov;

import com.openrecordsmanager.api.template.list.IListElement;
import com.openrecordsmanager.api.template.property.PropertyDefinition;
import com.openrecordsmanager.api.template.property.PropertyType;

import java.util.Collection;

public class RecordProperties {
    public static final PropertyDefinition<IListElement> RECORD_SECURITY_CLASSIFICATION =
            PropertyDefinition.builder("Record Security Classification", PropertyType.LIST_ITEM)
                    .description("Security classification of the record.")
                    .listType(Lists.SECURITY_CLASSIFICATION)
                    .securityFilter("principal[{0}] >= value", UserProperties.USER_SECURITY_CLASSIFICATION)
                    .build();

    public static final PropertyDefinition<Collection<IListElement>> RECORD_SECURITY_IMM =
            PropertyDefinition.builder("Record Information Management Marker", PropertyType.LIST_MULTIPLE)
                    .description("Standardised tags used to indicate specific legal, professional, or ethical restrictions on access and use.")
                    .listType(Lists.INFORMATION_MANAGEMENT_MARKER)
                    .validator("value.size() == 0 || resource[{0}] >= list('defaults_aus_gov:official_sensitive')", RECORD_SECURITY_CLASSIFICATION)
                    .build();

    public static final PropertyDefinition<Collection<IListElement>> RECORD_SECURITY_CAVEAT =
            PropertyDefinition.builder("Record Security Caveat", PropertyType.LIST_MULTIPLE)
                    .description("Warning that a security classified record or mandate requires special handling, " +
                            "and that only people cleared and briefed to see it may have access.")
                    .listType(Lists.SECURITY_CAVEAT)
                    .securityFilter("value.all(x, x in principal[{0}])", UserProperties.USER_SECURITY_CAVEAT)
                    .validator("value.size() == 0 || resource[{0}] >= list('defaults_aus_gov:protected')", RECORD_SECURITY_CLASSIFICATION)
                    .build();

    public static final PropertyDefinition<Collection<IListElement>> RECORD_SECURITY_RELEASABILITY =
            PropertyDefinition.builder("Record Releasability", PropertyType.LIST_MULTIPLE)
                    .description("Controls that dictate who is allowed to access, view, or receive specific classified or sensitive information.")
                    .listType(Lists.RELEASABILITY_CAVEAT)
                    .validator("value.size() > 2")
                    .build();

    public static final PropertyDefinition<IListElement> RECORD_CATEGORY =
            PropertyDefinition.builder("Record Category", PropertyType.LIST_ITEM)
                    .description("Specifies the specific category or aggregation of the record being described.")
                    .listType(Lists.RECORD_CATEGORY)
                    .build();

    public static final PropertyDefinition<String> JURISDICTION =
            PropertyDefinition.builder("Jurisdiction", PropertyType.STRING)
                    .description("Specification of a jurisdiction within which an entity operates, exists or is valid.")
                    .defaultValue("AU")
                    .build();
}
