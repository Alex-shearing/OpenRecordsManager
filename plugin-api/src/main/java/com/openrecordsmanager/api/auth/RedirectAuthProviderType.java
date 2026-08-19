package com.openrecordsmanager.api.auth;


import org.jspecify.annotations.Nullable;

import java.net.URI;

public abstract class RedirectAuthProviderType implements AuthProviderType {

    public abstract URI getRedirectTo(AuthProviderInstance instance);

    /**
     *
     * @param instance
     * @param context
     * @param uri
     * @return either {{@link UserAuthDetails}} or null if no user was authenticated
     */
    public abstract @Nullable UserAuthDetails authenticateCallback(
            AuthProviderInstance instance,
            UserAuthContext context,
            URI uri
    );
}
