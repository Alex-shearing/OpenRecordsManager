package com.openrecordsmanager.plugin.authlocal;

import com.openrecordsmanager.api.auth.AuthProviderInstance;
import com.openrecordsmanager.api.auth.InputAuthProviderType;
import com.openrecordsmanager.api.auth.UserDetails;
import com.openrecordsmanager.api.config.ConfigStore;
import org.jspecify.annotations.Nullable;

public class LocalAuthProviderType extends InputAuthProviderType {

    @Override
    public @Nullable UserDetails authenticate(ConfigStore config, AuthProviderInstance instance, String username, String password) {
        if (!config.getOrDefault(AuthLocalPlugin.CONFIG_ADMIN_ENABLED, false)) {
            return null;
        }
        return new UserDetails(instance, username, "");
    }
}
