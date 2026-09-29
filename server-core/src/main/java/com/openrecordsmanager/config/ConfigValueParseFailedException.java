package com.openrecordsmanager.config;

import com.openrecordsmanager.api.errors.ApiException;

public class ConfigValueParseFailedException extends ApiException {
    public static final String CODE = "config_value_parse_failed";

    public ConfigValueParseFailedException(String typeName, String attemptedValue) {
        super(CODE, typeName, attemptedValue);
    }
}
