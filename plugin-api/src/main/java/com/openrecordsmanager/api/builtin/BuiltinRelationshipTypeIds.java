package com.openrecordsmanager.api.builtin;

/**
 * Builtin relationship type item names.
 */
public final class BuiltinRelationshipTypeIds {

    // Location to location
    public static final String MEMBER_OF = "member_of";
    public static final String REPORTS_TO = "reports_to";
    public static final String DEPUTY_OF = "deputy_of";
    public static final String ACTING_FOR = "acting_for";

    // Record to record
    public static final String RELATED_TO = "related_to";
    public static final String SUPERSEDES = "supersedes";
    public static final String CONTAINED_BY = "contained_by";

    // Location to record
    public static final String CUSTODIAN_OF = "custodian_of";
    public static final String CREATED_BY = "created_by";

    private BuiltinRelationshipTypeIds() {
    }
}
