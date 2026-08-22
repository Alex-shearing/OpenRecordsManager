package com.openrecordsmanager.plugin.filestore_local;

import com.openrecordsmanager.api.filestore.FileStoreType;
import com.openrecordsmanager.api.schema.SchemaField;

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
    public String save(LocalFileStoreSettings settings, InputStream data) throws IOException {
        Path destPath;
        File file;
        do {
            destPath = this.getRandomPath();
            file = Path.of(settings.rootDir()).resolve(destPath).toFile();
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
        String hexString = HexFormat.of().toHexDigits(ThreadLocalRandom.current().nextLong());
        return Path.of(hexString.substring(0, 4), hexString.substring(4, 8), hexString.substring(8, 12), hexString.substring(12, 16));
    }

    @Override
    public InputStream retrieve(LocalFileStoreSettings settings, String data) throws IOException {
        File srcFile = new File(settings.rootDir(), data);
        if (!srcFile.exists()) {
            throw new IOException("File not found in local store: " + data);
        }
        return new FileInputStream(srcFile);
    }

    public record LocalFileStoreSettings(
            @SchemaField(title = "Root Directory", minLength = 1) String rootDir
    ) {
    }
}
