package com.openrecordsmanager.record.exception;

import com.openrecordsmanager.api.errors.ApiException;

public class DefaultFileStoreNotSetException extends ApiException {
    public DefaultFileStoreNotSetException() {
        super("default_file_store_not_set");
    }
}
