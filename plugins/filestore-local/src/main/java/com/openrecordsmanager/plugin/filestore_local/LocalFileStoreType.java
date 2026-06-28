package com.openrecordsmanager.plugin.filestore_local;

import com.openrecordsmanager.api.filestore.FileStoreType;

import java.io.*;

/**
 * An implementation of local file system storage.
 */
public class LocalFileStoreType extends FileStoreType<LocalFileStoreType.LocalFileStoreSettings> {

    public LocalFileStoreType() {
        super(LocalFileStoreSettings.class);
    }

    @Override
    public String id() {
        return "local";
    }

    @Override
    public void save(LocalFileStoreSettings properties, String path, InputStream data) throws IOException {
        File destFile = new File(properties.rootDir, path);
        if (!destFile.getParentFile().exists()) {
            destFile.getParentFile().mkdirs();
        }
        try (FileOutputStream out = new FileOutputStream(destFile)) {
            data.transferTo(out);
        }
    }

    @Override
    public InputStream retrieve(LocalFileStoreSettings properties, String path) throws IOException {
        File srcFile = new File(properties.rootDir, path);
        if (!srcFile.exists()) {
            throw new IOException("File not found in local store: " + path);
        }
        return new FileInputStream(srcFile);
    }

    public record LocalFileStoreSettings(String rootDir) {
    }
}
