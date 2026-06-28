package com.openrecordsmanager.api.filestore;

import com.openrecordsmanager.api.Component;
import java.io.InputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Defines a type of file store (e.g., local storage, Amazon S3) that can be provided by plugins.
 */
public abstract class FileStoreType implements Component {
    
    /**
     * Saves a file into the storage provider using the specified instance properties.
     *
     * @param properties configuration properties for the file store instance (e.g. root directory, bucket name)
     * @param path       the path or key where the file should be saved
     * @param data       the file contents stream
     * @throws IOException if there is an error saving the file
     */
    public abstract void save(Map<String, Object> properties, String path, InputStream data) throws IOException;

    /**
     * Retrieves a file from the storage provider using the specified instance properties.
     *
     * @param properties configuration properties for the file store instance (e.g. root directory, bucket name)
     * @param path       the path or key of the file to retrieve
     * @return an input stream of the file content
     * @throws IOException if there is an error retrieving the file
     */
    public abstract InputStream retrieve(Map<String, Object> properties, String path) throws IOException;
}
