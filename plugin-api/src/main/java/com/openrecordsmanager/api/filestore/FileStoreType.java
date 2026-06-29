package com.openrecordsmanager.api.filestore;

import com.openrecordsmanager.api.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Defines a type of file store (e.g., local storage, Amazon S3) that can be provided by plugins.
 */
public abstract class FileStoreType<T> implements Component {
    private final Class<T> propertiesClass;

    protected FileStoreType(Class<T> propertiesClass) {
        this.propertiesClass = propertiesClass;
    }

    /**
     * Saves a file into the storage provider using the specified instance properties.
     *
     * @param properties configuration properties for the file store instance (e.g. root directory, bucket name)
     * @param data       the file contents stream
     * @return the data to persist in the database. this same data will be used to retrieve the file.
     * @throws IOException if there is an error saving the file
     */
    public abstract String save(T properties, InputStream data) throws IOException;

    /**
     * Retrieves a file from the storage provider using the specified instance properties.
     *
     * @param properties configuration properties for the file store instance (e.g. root directory, bucket name)
     * @param data       the data or key of the file to retrieve
     * @return an input stream of the file content
     * @throws IOException if there is an error retrieving the file
     */
    public abstract InputStream retrieve(T properties, String data) throws IOException;

    public T parseOptions(Map<String, Object> properties) {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(properties, propertiesClass);
    }

    public Map<String, Object> serialiseOptions(T properties) {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(properties, Map.class);
    }
}
