package com.openrecordsmanager.api.auth;

import com.openrecordsmanager.api.Component;

public abstract class AuthProviderType<S extends Record> implements Component {
    private final Class<S> settingsClass;

    protected AuthProviderType(Class<S> settingsClass) {
        this.settingsClass = settingsClass;
    }

    public Class<S> getSettingsClass() {
        return this.settingsClass;
    }
}
