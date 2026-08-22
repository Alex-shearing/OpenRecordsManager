package com.openrecordsmanager.api.auth;

import com.openrecordsmanager.api.config.ConfigStore;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public abstract class InputAuthProviderType implements AuthProviderType {
    /**
     * Attempt to authenticate a user with the provided credential input.
     *
     * @param context  usable context to pull user information
     * @param instance the instance of the authentication provider being used
     * @param inputs   the user provided inputs
     * @return the {@link UserAuthDetails} or null if no user was authenticated
     */
    public abstract @Nullable UserAuthDetails authenticate(
            ConfigStore config,
            UserAuthContext context,
            AuthProviderInstance instance,
            Map<String, String> inputs
    );
}
