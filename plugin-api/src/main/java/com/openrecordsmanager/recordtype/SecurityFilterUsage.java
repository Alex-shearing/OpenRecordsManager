package com.openrecordsmanager.recordtype;

public enum SecurityFilterUsage {
    /**
     * Hides the fact the record exists at all from a user who does not pass the security filter.
     */
    HIDE_RECORD,
    /**
     * Allows the user to see that a record exists, but does not show them any record metadata or
     * view any attached files for a user who does not pass the security filter.
     */
    HIDE_METADATA_AND_FILES,
    /**
     * Hides attached files for a user who does not pass the security filter.
     */
    HIDE_FILES,
    /**
     * Does not hide any aspect of the record for a user who does not pass the security filter.
     */
    SHOW_ALL,
    ;

}
