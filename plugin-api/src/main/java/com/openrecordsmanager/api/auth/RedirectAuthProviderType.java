package com.openrecordsmanager.api.auth;


import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.Map;

public abstract class RedirectAuthProviderType<S extends Record> extends AuthProviderType<S> {

    protected RedirectAuthProviderType(Class<S> settingsClass) {
        super(settingsClass);
    }

    /**
     * Start a redirect login. The caller persists {@link RedirectAuthChallenge#state()} and
     * {@link RedirectAuthChallenge#attributes()} until the callback completes.
     *
     * @param callbackUri absolute redirect_uri registered with the IdP (no query string)
     * @param settings    the configured settings for the provider
     */
    protected abstract RedirectAuthChallenge begin(URI callbackUri, S settings);

    public final RedirectAuthChallenge beginUntyped(
            URI callbackUri,
            Map<String, ?> settings
    ) {
        return this.begin(callbackUri, this.parseSettings(settings));
    }

    /**
     * Complete a redirect login using the full callback URI (including query string) and the
     * pending state established by {@link #begin}.
     *
     * @return authenticated details, or null if authentication failed
     */
    protected abstract @Nullable UserAuthDetails complete(
            UserAuthContext context,
            URI fullCallbackUri,
            PendingRedirectAuth pending,
            S settings
    );

    public final @Nullable UserAuthDetails completeUntyped(
            UserAuthContext context,
            URI fullCallbackUri,
            PendingRedirectAuth pending,
            Map<String, ?> settings
    ) {
        return this.complete(
                context,
                fullCallbackUri,
                pending,
                this.parseSettings(settings)
        );
    }

}
