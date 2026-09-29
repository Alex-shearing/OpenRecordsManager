package com.openrecordsmanager.plugin.exception;

import com.openrecordsmanager.rest.exception.ResourceInUseException;

public class BuiltinResourceImmutableException extends ResourceInUseException {
    public BuiltinResourceImmutableException() {
        super("builtin_plugin_immutable", new String[0]);
    }
}
