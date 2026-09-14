package com.openrecordsmanager.plugin.filestoremiddleware_encrypting;

import com.openrecordsmanager.api.Plugin;
import com.openrecordsmanager.api.RegistrationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileStoreMiddlewareEncryptingPlugin implements Plugin {
    public static final Logger LOGGER = LoggerFactory.getLogger(FileStoreMiddlewareEncryptingPlugin.class);

    @Override
    public void initialise(RegistrationContext registry) {
        registry.registerComponent("encrypting", new FilestoreMiddlewareEncryptingType());
    }
}
