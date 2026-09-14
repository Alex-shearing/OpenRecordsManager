package com.openrecordsmanager.plugin.filestore_mssql_filestream;

import com.openrecordsmanager.api.Plugin;
import com.openrecordsmanager.api.RegistrationContext;

/**
 * Main plugin class that registers the Microsoft SQL Server FILESTREAM file store type.
 */
public class FileStoreMssqlFilestreamPlugin implements Plugin {
    @Override
    public void initialise(RegistrationContext registry) {
        registry.registerComponent("filestream", new MssqlFilestreamFileStoreType());
    }
}
