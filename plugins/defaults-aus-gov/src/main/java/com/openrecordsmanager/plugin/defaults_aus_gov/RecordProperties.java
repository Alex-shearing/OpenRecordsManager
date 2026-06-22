package com.openrecordsmanager.plugin.defaults_aus_gov;

import com.openrecordsmanager.api.list.IListElement;
import com.openrecordsmanager.api.property.PropertyDefinition;
import com.openrecordsmanager.api.property.PropertyType;

import java.util.List;

public class RecordProperties {
    public static final PropertyDefinition<IListElement> RECORD_SECURITY_CLASSIFICATION = PropertyDefinition.builder("record_security_classification", PropertyType.LIST_ITEM)
            .name("Record Security Classification")
            .description("Security classification of the record.")
            .listType(Lists.SECURITY_CLASSIFICATION)
            .securityFilter("principal[{0}] >= value", UserProperties.USER_SECURITY_CLASSIFICATION)
            .build();
    public static final PropertyDefinition<List<IListElement>> RECORD_SECURITY_IMM = PropertyDefinition.builder("record_security_imm", PropertyType.LIST_MULTIPLE)
            .name("Record Information Management Marker")
            .description("Standardised tags used to indicate specific legal, professional, or ethical restrictions on access and use.")
            .listType(Lists.INFORMATION_MANAGEMENT_MARKER)
            .validator("value.size() == 0 || resource[{0}] >= list('defaults_aus_gov:official_sensitive')", RECORD_SECURITY_CLASSIFICATION)
            .build();
    public static final PropertyDefinition<List<IListElement>> RECORD_SECURITY_CAVEAT = PropertyDefinition.builder("record_security_caveat", PropertyType.LIST_MULTIPLE)
            .name("Record Security Caveat")
            .description("Warning that a security classified record or mandate requires special handling, " +
                    "and that only people cleared and briefed to see it may have access.")
            .listType(Lists.SECURITY_CAVEAT)
            .securityFilter("value.all(x, x in principal[{0}])", UserProperties.USER_SECURITY_CAVEAT)
            .validator("value.size() == 0 || resource[{0}] >= list('defaults_aus_gov:protected')", RECORD_SECURITY_CLASSIFICATION)
            .build();
    public static final PropertyDefinition<List<IListElement>> RECORD_SECURITY_RELEASABILITY = PropertyDefinition.builder("record_security_releasability", PropertyType.LIST_MULTIPLE)
            .name("Record Releasability")
            .description("Controls that dictate who is allowed to access, view, or receive specific classified or sensitive information.")
            .listType(Lists.RELEASABILITY_CAVEAT)
            .validator("value.size() > 2")
            .build();
    public static final PropertyDefinition<IListElement> RECORD_CATEGORY = PropertyDefinition.builder("record_category", PropertyType.LIST_ITEM)
            .name("Record Category")
            .description("Specifies the specific category or aggregation of the record being described.")
            .listType(Lists.RECORD_CATEGORY)
            .build();
    public static final PropertyDefinition<String> JURISDICTION = PropertyDefinition.builder("jurisdiction", PropertyType.STRING)
            .name("Jurisdiction")
            .description("Specification of a jurisdiction within which an entity operates, exists or is valid.")
            .defaultValue("AU")
            .build();
}
