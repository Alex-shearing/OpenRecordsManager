package com.openrecordsmanager.api.builtin;

import com.openrecordsmanager.api.Plugin;
import com.openrecordsmanager.api.RegistrationContext;

public class BuiltinPlugin implements Plugin {

    @Override
    public String getName() {
        return "builtin";
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

        registry.registerComponent("notes", BuiltinProperties.NOTES);
        registry.registerComponent("date_registered", BuiltinProperties.DATE_REGISTERED);
        registry.registerComponent("date_created", BuiltinProperties.DATE_CREATED);
        registry.registerComponent("keywords", BuiltinProperties.KEYWORDS);
        registry.registerComponent("mime_type", BuiltinProperties.MIME_TYPE);
    }
}
