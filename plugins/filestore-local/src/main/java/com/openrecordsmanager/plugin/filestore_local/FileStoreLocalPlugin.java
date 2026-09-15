package com.openrecordsmanager.plugin.filestore_local;

import com.openrecordsmanager.api.Plugin;
import com.openrecordsmanager.api.RegistrationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main plugin class that registers the Local file store type component.
 */
public class FileStoreLocalPlugin implements Plugin {
    public static final Logger LOGGER = LoggerFactory.getLogger(FileStoreLocalPlugin.class);

    @Override
    public void initialise(RegistrationContext registry) {
        registry.registerComponent("local", new LocalFileStoreType());
    }
}
