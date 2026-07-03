package com.openrecordsmanager.plugin.filestore_s3;

import com.openrecordsmanager.api.Plugin;
import com.openrecordsmanager.api.PluginContext;

/**
 * Main plugin class that registers the S3 file store type component.
 */
public class FileStoreS3Plugin implements Plugin {

    public static final S3FileStoreType S3_FILE_STORE_TYPE = new S3FileStoreType();

    @Override
    public String getName() {
        return "filestore_s3";
    }

    @Override
    public void initialise(PluginContext registry) {
        registry.registerComponent("s3", S3_FILE_STORE_TYPE);
    }
}
