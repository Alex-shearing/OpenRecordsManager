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
                BuiltinConfigs.DATABASE_URL,
                BuiltinConfigs.WORKGROUP_NAME,
                BuiltinConfigs.DEFAULT_FILE_STORE,
                BuiltinConfigs.DEBUG_DETAILED_ERRORS,
                BuiltinConfigs.PLUGINS_DIRECTORY,
                BuiltinConfigs.PLUGINS_SKIP_STARTUP_CHECK
        );

        registry.registerComponent("notes", BuiltinProperties.NOTES);
        registry.registerComponent("date_registered", BuiltinProperties.DATE_REGISTERED);
        registry.registerComponent("date_created", BuiltinProperties.DATE_CREATED);
        registry.registerComponent("keywords", BuiltinProperties.KEYWORDS);
        registry.registerComponent("mime_type", BuiltinProperties.MIME_TYPE);
    }
}
