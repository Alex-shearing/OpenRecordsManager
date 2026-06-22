package com.openrecordsmanager.api.auth;

import com.openrecordsmanager.api.config.ConfigStore;

public abstract class InputAuthProviderType extends AuthProviderType {
    /**
     *
     * @param instance
     * @param username
     * @param password
     * @return the {@link UserDetails} or null if no user was authenticated
     */
    public abstract UserDetails authenticate(ConfigStore config, AuthProviderInstance instance, String username, String password);
}
