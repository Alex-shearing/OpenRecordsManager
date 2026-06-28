package com.openrecordsmanager.plugin.filestore_s3;

import com.openrecordsmanager.api.filestore.FileStoreType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * An implementation of S3 compatible file storage.
 */
public class S3FileStoreType extends FileStoreType {
    private static final Logger LOGGER = LoggerFactory.getLogger(S3FileStoreType.class);

    @Override
    public String id() {
        return "s3";
    }

    @Override
    public void save(Map<String, Object> properties, String path, InputStream data) throws IOException {
        String bucket = (String) properties.getOrDefault("bucket", "default-bucket");
        String endpoint = (String) properties.getOrDefault("endpoint", "s3.amazonaws.com");

        LOGGER.info("Uploading file to S3: endpoint={}, bucket={}, key={}", endpoint, bucket, path);

        // Simulating S3 compatible storage using a local mock directory
        File destFile = new File("./data/s3_mock/" + bucket, path);
        if (!destFile.getParentFile().exists()) {
            destFile.getParentFile().mkdirs();
        }
        try (FileOutputStream out = new FileOutputStream(destFile)) {
            data.transferTo(out);
        }
    }

    @Override
    public InputStream retrieve(Map<String, Object> properties, String path) throws IOException {
        String bucket = (String) properties.getOrDefault("bucket", "default-bucket");
        String endpoint = (String) properties.getOrDefault("endpoint", "s3.amazonaws.com");

        LOGGER.info("Downloading file from S3: endpoint={}, bucket={}, key={}", endpoint, bucket, path);

        File srcFile = new File("./data/s3_mock/" + bucket, path);
        if (!srcFile.exists()) {
            throw new IOException("S3 Object not found: bucket=" + bucket + ", key=" + path);
        }
        return new FileInputStream(srcFile);
    }
}
