package com.openrecordsmanager.plugin.filestore_local;

import com.openrecordsmanager.api.Plugin;
import com.openrecordsmanager.api.PluginContext;

/**
 * Main plugin class that registers the Local file store type component.
 */
public class FileStoreLocalPlugin implements Plugin {

    public static final LocalFileStoreType LOCAL_FILE_STORE_TYPE = new LocalFileStoreType();

    @Override
    public String getName() {
        return "filestore_local";
    }

    @Override
    public void initialise(PluginContext registry) {
        registry.registerComponents(LOCAL_FILE_STORE_TYPE);
    }
}
