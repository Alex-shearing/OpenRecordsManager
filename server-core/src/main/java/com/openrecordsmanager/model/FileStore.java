package com.openrecordsmanager.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openrecordsmanager.resources.ResourceIdentifier;
import jakarta.persistence.*;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "file_store")
public class FileStore {
    private static final String CURRENT_HASH_ALGORITHM = "SHA-256";

    @Id
    @JsonProperty
    public UUID id;

    @Column(nullable = false)
    @JsonProperty
    public ResourceIdentifier type;

    @Column(nullable = false)
    @JsonProperty
    public Map<String, Object> properties;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "store")
    public Set<FileStoreEntry> files;

    @Deprecated
    protected FileStore() {
    }

    public FileStore(ResourceIdentifier type, Map<String, Object> properties) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.properties = properties;
    }

    public FileStoreEntry newFile(InputStream file) {
        byte[] hashBytes;
        long length = 0;
        try {
            MessageDigest md = MessageDigest.getInstance(CURRENT_HASH_ALGORITHM);
            // Use a DigestInputStream to automatically update the MessageDigest as the file is read
            try (DigestInputStream dis = new DigestInputStream(file, md)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = dis.read(buffer)) != -1) {
                    length += bytesRead;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            hashBytes = md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to initialize SHA-256 algorithm for file hashing.");
        }

        // Convert the byte array into a hexadecimal string
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return new FileStoreEntry(this, "/todo", CURRENT_HASH_ALGORITHM, hexString.toString(), length);
    }
}
