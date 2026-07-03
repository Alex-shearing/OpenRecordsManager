package com.openrecordsmanager.plugin.authlocal;

import com.openrecordsmanager.api.auth.AuthProviderInstance;
import com.openrecordsmanager.api.auth.InputAuthProviderType;
import com.openrecordsmanager.api.auth.UserDetails;
import com.openrecordsmanager.api.config.ConfigStore;

public class LocalAuthProviderType extends InputAuthProviderType {

    @Override
    public UserDetails authenticate(ConfigStore config, AuthProviderInstance instance, String username, String password) {
        if (!config.getProperty(AuthLocalPlugin.CONFIG_ADMIN_ENABLED, false)) {
            return null;
        }
        return new UserDetails(instance, username, "");
    }

    @Override
    public String id() {
        return "local_auth";
    }
}
