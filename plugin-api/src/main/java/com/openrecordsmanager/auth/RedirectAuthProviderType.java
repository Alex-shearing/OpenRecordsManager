package com.openrecordsmanager.auth;


import java.net.URI;

public abstract class RedirectAuthProviderType extends AuthProviderType {

    public abstract URI getRedirectTo(AuthProviderInstance instance);

    /**
     *
     * @param instance
     * @param uri
     * @return either {{@link UserDetails}} or null if no user was authenticated
     */
    public abstract UserDetails authenticateCallback(AuthProviderInstance instance, URI uri);
}
