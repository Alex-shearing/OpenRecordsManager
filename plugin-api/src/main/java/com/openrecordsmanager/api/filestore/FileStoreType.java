package com.openrecordsmanager.api.filestore;

import com.openrecordsmanager.api.Component;
import com.openrecordsmanager.api.schema.JsonSchemaValidator;
import org.jspecify.annotations.Nullable;

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
     * @param settings  configuration properties for the file store instance (e.g. root directory, bucket name)
     * @param data      the file contents stream
     * @param extension file extension without a leading dot (e.g. {@code "pdf"}, {@code "jar"});
     *                  may be {@code null} or blank when unknown
     * @return the data to persist in the database. this same data will be used to retrieve the file.
     * @throws IOException if there is an error saving the file
     */
    public abstract String save(S settings, InputStream data, @Nullable String extension) throws IOException;

    /**
     * Saves a file using untyped settings (typically raw JSON maps from persistence).
     */
    public final String saveUntyped(Map<String, ?> settings, InputStream data, @Nullable String extension)
            throws IOException {
        return this.save(this.parseSettings(settings), data, extension);
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
    public final InputStream retrieveUntyped(Map<String, ?> properties, String data) throws IOException {
        return this.retrieve(this.parseSettings(properties), data);
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
