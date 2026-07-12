package com.openrecordsmanager.plugin.authlocal;

import com.openrecordsmanager.api.auth.AuthProviderInstance;
import com.openrecordsmanager.api.auth.InputAuthProviderType;
import com.openrecordsmanager.api.auth.UserAuthDetails;
import com.openrecordsmanager.api.config.ConfigStore;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public class LocalAuthProviderType extends InputAuthProviderType {

    @Override
    public @Nullable UserAuthDetails authenticate(ConfigStore config, AuthProviderInstance instance, Map<String, String> inputs) {
        if (!config.getOrDefault(AuthLocalPlugin.CONFIG_ADMIN_ENABLED, false)) {
            return null;
        }

        return new UserAuthDetails(instance, inputs.get("username"), "");
    }
}
