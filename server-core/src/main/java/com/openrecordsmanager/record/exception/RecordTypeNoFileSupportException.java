package com.openrecordsmanager.record.exception;

import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.errors.ApiException;

public class RecordTypeNoFileSupportException extends ApiException {
    public RecordTypeNoFileSupportException(ResourceIdentifier recordTypeId) {
        super("record_type_no_file_support", recordTypeId.toString());
    }
}
