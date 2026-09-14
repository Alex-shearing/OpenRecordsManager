package com.openrecordsmanager.plugin.filestore_local;

import com.openrecordsmanager.api.Plugin;
import com.openrecordsmanager.api.RegistrationContext;

/**
 * Main plugin class that registers the Local file store type component.
 */
public class FileStoreLocalPlugin implements Plugin {
    @Override
    public void initialise(RegistrationContext registry) {
        registry.registerComponent("local", new LocalFileStoreType());
    }
}
