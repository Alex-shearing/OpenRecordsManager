package com.openrecordsmanager.record.dto;

import com.openrecordsmanager.filestore.store.FileStoreEntry;
import com.openrecordsmanager.plugin.registry.ComponentCatalog;
import com.openrecordsmanager.record.RecordRevision;
import org.springframework.http.ContentDisposition;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public record RecordRevisionResponse(
        InputStream stream,
        String version,
        String extension,
        long sizeBytes,
        String hash,
        String hashAlgorithm
) {
    public static RecordRevisionResponse of(ComponentCatalog catalog, RecordRevision revision) {
        FileStoreEntry file = revision.file;
        return new RecordRevisionResponse(
                file.getFile(catalog),
                revision.version,
                file.extension,
                file.sizeBytes,
                file.hash,
                file.hashAlgorithm
        );
    }

    private String getFileName(String name) {
        if (this.extension != null) {
            name += "." + this.extension;
        }
        return name;
    }

    public String digestHeader() {
        return String.format("%s=:%s:",
                this.hashAlgorithm().toLowerCase(Locale.ROOT),
                this.hash()
        );
    }

    public ContentDisposition dispositionHeader() {
        return ContentDisposition.attachment()
                .filename(this.getFileName(this.version()), StandardCharsets.UTF_8)
                .build();
    }
}
