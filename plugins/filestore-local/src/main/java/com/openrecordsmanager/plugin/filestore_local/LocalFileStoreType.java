package com.openrecordsmanager.plugin.filestore_local;

import com.openrecordsmanager.api.filestore.FileStoreType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HexFormat;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Implementation of the file system storage.
 */
public class LocalFileStoreType extends FileStoreType<LocalFileStoreType.LocalFileStoreSettings> {

    public LocalFileStoreType() {
        super(LocalFileStoreSettings.class);
    }

    @Override
    public String save(LocalFileStoreSettings settings, InputStream data, @Nullable String extension) throws IOException {
        Path relativePath;
        Path absolutePath;
        do {
            relativePath = this.getRandomPath();
            absolutePath = Path.of(settings.rootDir()).toAbsolutePath().resolve(relativePath).toAbsolutePath();
        } while (Files.exists(absolutePath));

        FileStoreLocalPlugin.LOGGER.debug("Saving new file to destination path {}", relativePath);

        Files.createDirectories(absolutePath.getParent());

        try (OutputStream out = Files.newOutputStream(absolutePath, StandardOpenOption.CREATE_NEW)) {
            data.transferTo(out);
        } catch (FileAlreadyExistsException e) {
            FileStoreLocalPlugin.LOGGER.error(
                    "Attempted to write to a file that already exists in the store ({}).",
                    absolutePath,
                    e
            );
            throw e;
        }

        FileStoreLocalPlugin.LOGGER.debug("File saved to destination path {} in store", absolutePath);

        return relativePath.toString();
    }

    private Path getRandomPath() {
        String hexString = HexFormat.of().toHexDigits(ThreadLocalRandom.current().nextLong());
        return Path.of(
                hexString.substring(0, 4),
                hexString.substring(4, 8),
                hexString.substring(8, 12),
                hexString.substring(12, 16)
        );
    }

    @Override
    public InputStream retrieve(LocalFileStoreSettings settings, String data) throws IOException {
        Path srcFile = Path.of(settings.rootDir()).toAbsolutePath().resolve(data);

        FileStoreLocalPlugin.LOGGER.debug("Retrieving file from destination path {}", srcFile);

        if (!Files.exists(srcFile)) {
            throw new IOException("File not found in local store: " + data);
        }
        return Files.newInputStream(srcFile);
    }

    public record LocalFileStoreSettings(
            @Schema(title = "Root Directory") @NotBlank String rootDir
    ) {
    }
}
