package com.openrecordsmanager.api.filestore;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.schema.JsonSchemaValidator;
import com.openrecordsmanager.api.schema.RecordInputs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Defines a type of file store (e.g., local storage, Amazon S3) that can be provided by plugins.
 */
public abstract class FileStoreType<S extends Record> implements Component {
    private final Class<S> settingsClass;

    protected FileStoreType(Class<S> settingsClass) {
        this.settingsClass = settingsClass;
    }

    /**
     * Saves a file into the storage provider using the specified instance properties.
     *
     * @param settings configuration properties for the file store instance (e.g. root directory, bucket name)
     * @param data     the file contents stream
     * @return the data to persist in the database. this same data will be used to retrieve the file.
     * @throws IOException if there is an error saving the file
     */
    public abstract String save(S settings, InputStream data) throws IOException;

    /**
     * Saves a file using untyped properties (typically raw JSON maps from persistence).
     */
    public final String saveUntyped(Object properties, InputStream data) throws IOException {
        return this.save(this.parseSettings(properties), data);
    }

    /**
     * Retrieves a file from the storage provider using the specified instance properties.
     *
     * @param settings configuration properties for the file store instance (e.g. root directory, bucket name)
     * @param data     the data or key of the file to retrieve
     * @return an input stream of the file content
     * @throws IOException if there is an error retrieving the file
     */
    public abstract InputStream retrieve(S settings, String data) throws IOException;

    /**
     * Retrieves a file using untyped properties (typically raw JSON maps from persistence).
     */
    public final InputStream retrieveUntyped(Object properties, String data) throws IOException {
        return this.retrieve(this.parseSettings(properties), data);
    }

    public S parseSettings(Object properties) {
        return RecordInputs.parse(this.settingsClass, properties);
    }

    public final Map<String, Object> validateSettings(Map<String, ?> properties) {
        return JsonSchemaValidator.validate(this.settingsClass, properties);
    }
}
