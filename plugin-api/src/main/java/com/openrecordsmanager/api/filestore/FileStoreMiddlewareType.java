package com.openrecordsmanager.api.filestore;

import com.openrecordsmanager.api.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;

/**
 * Defines a middleware that can modify the files before writing and reading from the file store.
 */
public abstract class FileStoreMiddlewareType<T> implements Component {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Class<T> propertiesClass;

    protected FileStoreMiddlewareType(Class<T> propertiesClass) {
        this.propertiesClass = propertiesClass;
    }

    /**
     * Applies modification to the InputStream during save.
     *
     * @param properties configuration properties for the middleware instance
     * @param data       the file contents stream
     * @return the data to persist in the database. this same data will be used to retrieve the file.
     */
    public abstract InputStream duringSave(T properties, InputStream data);

    /**
     * Retrieves the modified InputStream and converts it back to the original InputStream
     *
     * @param properties configuration properties for the file store instance (e.g. root directory, bucket name)
     * @param data       the file contents stream
     * @return an input stream of the file content
     */
    public abstract InputStream duringRetrieve(T properties, InputStream data);

    public T parseOptions(Object properties) {
        return MAPPER.convertValue(properties, propertiesClass);
    }
}
