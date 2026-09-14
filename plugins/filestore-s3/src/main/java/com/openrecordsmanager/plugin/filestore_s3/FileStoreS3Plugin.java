package com.openrecordsmanager.plugin.filestore_s3;

import com.openrecordsmanager.api.Plugin;
import com.openrecordsmanager.api.RegistrationContext;

/**
 * Main plugin class that registers the S3 file store type component.
 */
public class FileStoreS3Plugin implements Plugin {
    @Override
    public void initialise(RegistrationContext registry) {
        registry.registerComponent("s3", new S3FileStoreType());
    }
}
