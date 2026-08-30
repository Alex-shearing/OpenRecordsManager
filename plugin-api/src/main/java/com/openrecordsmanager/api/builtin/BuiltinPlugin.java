package com.openrecordsmanager.api.builtin;

import com.openrecordsmanager.api.Plugin;
import com.openrecordsmanager.api.RegistrationContext;
import com.openrecordsmanager.api.ResourceIdentifier;
import com.openrecordsmanager.api.template.property.ObjectPropertyTemplate;

public class BuiltinPlugin implements Plugin {

    public static final String BUILTIN_PLUGIN_NAME = "builtin";

    @Override
    public String getName() {
        return BUILTIN_PLUGIN_NAME;
    }

    @Override
    public void initialise(RegistrationContext registry) {
        registry.registerConfig(
                BuiltinConfigs.DATABASE_PRIMARY,
                BuiltinConfigs.DATABASE_READ_ONLY,
                BuiltinConfigs.WORKGROUP_NAME,
                BuiltinConfigs.DEFAULT_FILE_STORE,
                BuiltinConfigs.DEBUG_DETAILED_ERRORS,
                BuiltinConfigs.DEBUG_SHOW_SQL,
                BuiltinConfigs.DATABASE_PROBE_INTERVAL_MS,
                BuiltinConfigs.CORS_ALLOWED_ORIGINS,
                BuiltinConfigs.CORS_ALLOWED_HEADERS,
                BuiltinConfigs.COOKIE_NAME,
                BuiltinConfigs.TOKEN_EXPIRATION_TIME,
                BuiltinConfigs.COOKIE_SECURE,
                BuiltinConfigs.PLUGINS_DIRECTORY,
                BuiltinConfigs.PLUGINS_SKIP_STARTUP_CHECK,
                BuiltinConfigs.WEB_DIRECTORY,
                BuiltinConfigs.WEB_PRODUCT_NAME,
                BuiltinConfigs.WEB_LOGO_URL,
                BuiltinConfigs.WEB_FAVICON_URL,
                BuiltinConfigs.WEB_PRIMARY_COLOR,
                BuiltinConfigs.WEB_SUPPORT_URL,
                BuiltinConfigs.AUDIT_ENABLED,
                BuiltinConfigs.AUDIT_SPOOL_DIRECTORY,
                BuiltinConfigs.AUDIT_SPOOL_DRAIN_INTERVAL_SECONDS,
                BuiltinConfigs.AUDIT_FILE_ARCHIVE_ENABLED
        );

        registerProperty(registry, BuiltinProperties.NOTES_ID, BuiltinProperties.NOTES);
        registerProperty(registry, BuiltinProperties.DATE_REGISTERED_ID, BuiltinProperties.DATE_REGISTERED);
        registerProperty(registry, BuiltinProperties.DATE_CREATED_ID, BuiltinProperties.DATE_CREATED);
        registerProperty(registry, BuiltinProperties.KEYWORDS_ID, BuiltinProperties.KEYWORDS);
        registerProperty(registry, BuiltinProperties.MIME_TYPES_ID, BuiltinProperties.MIME_TYPES);
        registerProperty(registry, BuiltinProperties.TITLE_ID, BuiltinProperties.TITLE);
        registerProperty(registry, BuiltinProperties.DATE_MODIFIED_ID, BuiltinProperties.DATE_MODIFIED);
        registerProperty(registry, BuiltinProperties.GIVEN_NAME_ID, BuiltinProperties.GIVEN_NAME);
        registerProperty(registry, BuiltinProperties.SURNAME_ID, BuiltinProperties.SURNAME);
        registerProperty(registry, BuiltinProperties.HONORIFIC_ID, BuiltinProperties.HONORIFIC);
        registerProperty(registry, BuiltinProperties.EMAIL_ID, BuiltinProperties.EMAIL);
    }

    private static void registerProperty(
            RegistrationContext registry,
            ResourceIdentifier id,
            ObjectPropertyTemplate<?> template
    ) {
        registry.registerComponent(id.item(), template);
    }
}
