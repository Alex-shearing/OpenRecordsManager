package com.openrecordsmanager.api.filestore;

import com.openrecordsmanager.api.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;

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
     * @param path       the path or key where the file should be saved
     * @param data       the file contents stream
     * @throws IOException if there is an error saving the file
     */
    public abstract void save(T properties, String path, InputStream data) throws IOException;

    /**
     * Retrieves a file from the storage provider using the specified instance properties.
     *
     * @param properties configuration properties for the file store instance (e.g. root directory, bucket name)
     * @param path       the path or key of the file to retrieve
     * @return an input stream of the file content
     * @throws IOException if there is an error retrieving the file
     */
    public abstract InputStream retrieve(T properties, String path) throws IOException;

    public T parseOptions(ObjectNode properties) {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.treeToValue(properties, propertiesClass);
    }
}
