package com.openrecordsmanager;

import com.openrecordsmanager.auth.AuthProviderInstance;
import com.openrecordsmanager.auth.InputAuthProviderType;
import com.openrecordsmanager.auth.UserDetails;
import com.openrecordsmanager.config.ConfigStore;

public class LocalAuthProviderType extends InputAuthProviderType {

    @Override
    public UserDetails authenticate(ConfigStore config, AuthProviderInstance instance, String username, String password) {
        if (!config.getProperty(AuthLocalPlugin.CONFIG_ADMIN_ENABLED)) {
            return null;
        }
        return new UserDetails(instance, username, "");
    }

    @Override
    public String id() {
        return "local_auth";
    }
}
