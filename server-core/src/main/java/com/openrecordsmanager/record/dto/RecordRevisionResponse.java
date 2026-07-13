package com.openrecordsmanager.record.dto;

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
        String hashAlgorithm) {

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
