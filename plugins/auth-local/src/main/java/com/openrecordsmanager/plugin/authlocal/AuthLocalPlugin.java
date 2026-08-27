package com.openrecordsmanager.plugin.authlocal;

import com.openrecordsmanager.api.Plugin;
import com.openrecordsmanager.api.RegistrationContext;
import com.openrecordsmanager.api.template.property.ObjectPropertyTemplate;
import com.openrecordsmanager.api.template.property.PropertyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthLocalPlugin implements Plugin {
    public static final Logger LOGGER = LoggerFactory.getLogger(AuthLocalPlugin.class);
    
    public static final ObjectPropertyTemplate<String> PASSWORD_HASH_PROPERTY = ObjectPropertyTemplate.builder("Password Hash", PropertyType.STRING)
            .description("Hashed password for the user")
            .userHidden()
            .build();

    @Override
    public String getName() {
        return "auth_local";
    }

    @Override
    public void initialise(RegistrationContext registry) {
        LOGGER.info("Initializing plugin...");

        registry.registerComponent("password_hash", PASSWORD_HASH_PROPERTY);
        registry.registerComponent("local_auth", new LocalAuthProviderType());
        registry.registerComponent("reset_password", new ResetLocalPasswordAction());
    }
}
