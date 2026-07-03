package com.openrecordsmanager.plugin.filestore_local;

import com.openrecordsmanager.api.filestore.FileStoreType;

import java.io.*;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.concurrent.ThreadLocalRandom;

/**
 * An implementation of local file system storage.
 */
public class LocalFileStoreType extends FileStoreType<LocalFileStoreType.LocalFileStoreSettings> {

    public LocalFileStoreType() {
        super(LocalFileStoreSettings.class);
    }
    
    @Override
    public String save(LocalFileStoreSettings properties, InputStream data) throws IOException {
        Path destPath;
        File file;
        do {
            destPath = this.getRandomPath();
            file = Path.of(properties.rootDir).resolve(destPath).toFile();
        } while (file.exists());

        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try (FileOutputStream out = new FileOutputStream(file)) {
            data.transferTo(out);
        }
        return destPath.toString();
    }

    private Path getRandomPath() {
        // Generate a random 16 character string to use as the key
        String hexString = HexFormat.of().toHexDigits(ThreadLocalRandom.current().nextLong());
        // break the string into parts to break into directories 0123/4567/89AB/CDEF
        return Path.of(hexString.substring(0, 4), hexString.substring(4, 8), hexString.substring(8, 12), hexString.substring(12, 16));
    }

    @Override
    public InputStream retrieve(LocalFileStoreSettings properties, String data) throws IOException {
        File srcFile = new File(properties.rootDir, data);
        if (!srcFile.exists()) {
            throw new IOException("File not found in local store: " + data);
        }
        return new FileInputStream(srcFile);
    }

    public record LocalFileStoreSettings(String rootDir) {
    }
}
