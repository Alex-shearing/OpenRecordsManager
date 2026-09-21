package com.openrecordsmanager.api.auth;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.schema.JsonSchemaValidator;

import java.util.Map;

public abstract class AuthProviderType<S extends Record> implements Component {
    private final Class<S> settingsClass;

    protected AuthProviderType(Class<S> settingsClass) {
        this.settingsClass = settingsClass;
    }

    public Class<S> getSettingsClass() {
        return this.settingsClass;
    }

    public S parseSettings(Map<String, ?> properties) {
        return JsonSchemaValidator.toRecord(this.settingsClass, properties);
    }
}
