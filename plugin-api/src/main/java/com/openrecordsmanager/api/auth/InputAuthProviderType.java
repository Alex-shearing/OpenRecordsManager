package com.openrecordsmanager.api.auth;

import com.openrecordsmanager.api.config.ConfigStore;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public abstract class InputAuthProviderType implements AuthProviderType {
    /**
     *
     * @param context
     * @param instance
     * @param inputs
     * @return the {@link UserAuthDetails} or null if no user was authenticated
     */
    public abstract @Nullable UserAuthDetails authenticate(
            ConfigStore config,
            UserAuthContext context,
            AuthProviderInstance instance,
            Map<String, String> inputs
    );
}
