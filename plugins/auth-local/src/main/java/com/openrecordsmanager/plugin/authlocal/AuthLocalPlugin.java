package com.openrecordsmanager.plugin.authlocal;

import com.openrecordsmanager.api.ComponentReference;
import com.openrecordsmanager.api.Plugin;
import com.openrecordsmanager.api.RegistrationContext;
import com.openrecordsmanager.api.template.property.ObjectPropertyTemplate;
import com.openrecordsmanager.api.template.property.PropertyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthLocalPlugin implements Plugin {
    public static final Logger LOGGER = LoggerFactory.getLogger(AuthLocalPlugin.class);

    @Override
    public void initialise(RegistrationContext registry) {
        LOGGER.info("Initializing plugin...");

        ComponentReference<ObjectPropertyTemplate<String>> property = registry.registerComponent(
                "password_hash",
                ObjectPropertyTemplate.builder(PropertyType.STRING)
                        .systemProperty()
                        .build()
        );

        registry.registerComponent("local_auth", new LocalAuthProviderType(property));
        registry.registerComponent("reset_password", new ResetLocalPasswordAction(property));
    }
}
