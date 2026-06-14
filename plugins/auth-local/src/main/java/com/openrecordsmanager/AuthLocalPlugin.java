package com.openrecordsmanager;

import com.openrecordsmanager.config.ConfigDefinition;
import com.openrecordsmanager.config.ConfigValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthLocalPlugin implements Plugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthLocalPlugin.class);

    public static final LocalAuthProviderType LOCAL_AUTH_PROVIDER_TYPE = new LocalAuthProviderType();

    public static final ConfigDefinition<Boolean> CONFIG_ADMIN_ENABLED = ConfigDefinition.builder("auth.auth_local.enable_default_admin", ConfigValueType.BOOL)
            .name("Enable Default Admin Account")
            .description("Enables the default admin account with basic, well known credentials.")
            .defaultValue(false)
            .build();

    @Override
    public String getName() {
        return "auth_local";
    }

    @Override
    public void initialise(PluginContext registry) {
        LOGGER.info("Initializing plugin...");

        registry.registerComponents(CONFIG_ADMIN_ENABLED, LOCAL_AUTH_PROVIDER_TYPE);
    }
}
