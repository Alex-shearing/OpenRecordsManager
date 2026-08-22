package com.openrecordsmanager.plugin.filestore_s3;

import com.openrecordsmanager.api.filestore.FileStoreType;
import com.openrecordsmanager.api.schema.SchemaField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.UUID;

/**
 * An implementation of S3 compatible file storage.
 */
public class S3FileStoreType extends FileStoreType<S3FileStoreType.S3FileStoreSettings> {
    private static final Logger LOGGER = LoggerFactory.getLogger(S3FileStoreType.class);

    public S3FileStoreType() {
        super(S3FileStoreSettings.class);
    }

    @Override
    public String save(S3FileStoreSettings settings, InputStream data) throws IOException {
        String path = UUID.randomUUID().toString();

        LOGGER.info("Uploading file to S3: endpoint={}, bucket={}, key={}", settings.endpoint(), settings.bucket(), path);

        // Simulating S3 compatible storage using a local mock directory
        File destFile = new File("./data/s3_mock/" + settings.bucket(), path);
        if (!destFile.getParentFile().exists()) {
            destFile.getParentFile().mkdirs();
        }
        try (FileOutputStream out = new FileOutputStream(destFile)) {
            data.transferTo(out);
        }

        return path;
    }

    @Override
    public InputStream retrieve(S3FileStoreSettings settings, String data) throws IOException {
        LOGGER.info("Downloading file from S3: endpoint={}, bucket={}, key={}", settings.endpoint(), settings.bucket(), data);

        File srcFile = new File("./data/s3_mock/" + settings.bucket(), data);
        if (!srcFile.exists()) {
            throw new IOException("S3 Object not found: bucket=" + settings.bucket() + ", key=" + data);
        }
        return new FileInputStream(srcFile);
    }

    public record S3FileStoreSettings(
            @SchemaField(title = "Bucket", minLength = 1) String bucket,
            @SchemaField(title = "Endpoint", minLength = 1) String endpoint
    ) {
    }
}
