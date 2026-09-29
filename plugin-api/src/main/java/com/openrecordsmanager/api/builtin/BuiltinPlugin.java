package com.openrecordsmanager.api.builtin;

import com.openrecordsmanager.api.Plugin;
import com.openrecordsmanager.api.RegistrationContext;

public class BuiltinPlugin implements Plugin {
    public static final String BUILTIN_PLUGIN_NAME = "builtin";

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
                BuiltinConfigs.REFRESH_COOKIE_NAME,
                BuiltinConfigs.JWT_SIGNING_KEY,
                BuiltinConfigs.ACCESS_TOKEN_EXPIRATION_SECONDS,
                BuiltinConfigs.DEGRADED_ACCESS_TOKEN_EXPIRATION_SECONDS,
                BuiltinConfigs.REFRESH_TOKEN_EXPIRATION_SECONDS,
                BuiltinConfigs.DEGRADED_REFRESH_TOKEN_EXPIRATION_SECONDS,
                BuiltinConfigs.COOKIE_SECURE,
                BuiltinConfigs.PLUGINS_DIRECTORY,
                BuiltinConfigs.PLUGINS_SKIP_SYNC,
                BuiltinConfigs.PLUGINS_SYNC_INTERVAL_MS,
                BuiltinConfigs.WEB_DIRECTORY,
                BuiltinConfigs.MULTIPART_MAX_FILE_SIZE,
                BuiltinConfigs.MULTIPART_MAX_REQUEST_SIZE,
                BuiltinConfigs.WEB_LOGO_URL,
                BuiltinConfigs.WEB_FAVICON_URL,
                BuiltinConfigs.WEB_PRIMARY_COLOR,
                BuiltinConfigs.WEB_SUPPORT_URL,
                BuiltinConfigs.AUDIT_ENABLED,
                BuiltinConfigs.AUDIT_SPOOL_DIRECTORY,
                BuiltinConfigs.AUDIT_SPOOL_DRAIN_INTERVAL_SECONDS,
                BuiltinConfigs.AUDIT_FILE_ARCHIVE_ENABLED
        );

        BuiltinProperties.BUILTIN_PROPERTIES.forEach((i, template) ->
                registry.registerComponent(i.item(), template)
        );
    }
}
