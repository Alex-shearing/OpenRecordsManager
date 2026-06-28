package com.openrecordsmanager.plugin.filestore_local;

import com.openrecordsmanager.api.filestore.FileStoreType;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * An implementation of local file system storage.
 */
public class LocalFileStoreType extends FileStoreType {

    @Override
    public String id() {
        return "local";
    }

    @Override
    public void save(Map<String, Object> properties, String path, InputStream data) throws IOException {
        String rootDir = (String) properties.getOrDefault("root_dir", "./data/files");
        File destFile = new File(rootDir, path);
        if (!destFile.getParentFile().exists()) {
            destFile.getParentFile().mkdirs();
        }
        try (FileOutputStream out = new FileOutputStream(destFile)) {
            data.transferTo(out);
        }
    }

    @Override
    public InputStream retrieve(Map<String, Object> properties, String path) throws IOException {
        String rootDir = (String) properties.getOrDefault("root_dir", "./data/files");
        File srcFile = new File(rootDir, path);
        if (!srcFile.exists()) {
            throw new IOException("File not found in local store: " + path);
        }
        return new FileInputStream(srcFile);
    }
}
