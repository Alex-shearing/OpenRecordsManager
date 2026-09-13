package com.openrecordsmanager.api.filestore;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.schema.JsonSchemaValidator;

import java.io.InputStream;
import java.util.Map;

/**
 * Defines a middleware that can modify the files before writing and reading from the file store.
 */
public abstract class FileStoreMiddlewareType<S extends Record> implements Component {
    private final Class<S> settingsClass;

    protected FileStoreMiddlewareType(Class<S> settingsClass) {
        this.settingsClass = settingsClass;
    }

    /**
     * Applies modification to the InputStream during save.
     *
     * @param settings configuration properties for the middleware instance
     * @param data     the file contents stream
     * @return the data to persist in the database. this same data will be used to retrieve the file.
     */
    public abstract InputStream duringSave(S settings, InputStream data);

    /**
     * Applies save-time modification using untyped properties (typically raw JSON maps from persistence).
     */
    public final InputStream duringSaveUntyped(Map<String, ?> properties, InputStream data) {
        return this.duringSave(this.parseSettings(properties), data);
    }

    /**
     * Retrieves the modified InputStream and converts it back to the original InputStream
     *
     * @param settings configuration properties for the file store instance (e.g. root directory, bucket name)
     * @param data     the file contents stream
     * @return an input stream of the file content
     */
    public abstract InputStream duringRetrieve(S settings, InputStream data);

    /**
     * Applies retrieve-time modification using untyped properties (typically raw JSON maps from persistence).
     */
    public final InputStream duringRetrieveUntyped(Map<String, ?> properties, InputStream data) {
        return this.duringRetrieve(this.parseSettings(properties), data);
    }

    /**
     * Validates settings against the live backend and performs any one-time setup
     * required before this instance can be used. Invoked before persisting create/update.
     */
    public void initialize(S settings) {
    }

    /**
     * Initializes using untyped settings (typically raw JSON maps from the API).
     */
    public final void initializeUntyped(Map<String, ?> settings) {
        this.initialize(this.parseSettings(settings));
    }

    public S parseSettings(Map<String, ?> properties) {
        return JsonSchemaValidator.toRecord(this.settingsClass, properties);
    }

    public Class<S> getSettingsClass() {
        return this.settingsClass;
    }
}
