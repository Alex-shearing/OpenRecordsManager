package com.openrecordsmanager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthOidcPlugin implements Plugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthOidcPlugin.class);

    public static final OidcAuthProviderType OIDC_AUTH_PROVIDER_TYPE = new OidcAuthProviderType();

    @Override
    public String getName() {
        return "auth_oidc";
    }

    @Override
    public void initialise(PluginContext registry) {
        LOGGER.info("Initializing plugin...");

        registry.registerComponents(OIDC_AUTH_PROVIDER_TYPE);
    }
}
