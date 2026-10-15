package com.openrecordsmanager.plugin.filestoremiddleware_encrypting;

import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import java.security.spec.AlgorithmParameterSpec;

public enum EncryptionType {
    AES(1, "AES/GCM/NoPadding", "AES", 12, true);

    private final int id;
    private final String transformation;
    private final String secretKeySpec;
    private final int ivLength;
    private final boolean isGcm;

    EncryptionType(int id, String transformation, String secretKeySpec, int ivLength, boolean isGcm) {
        this.id = id;
        this.transformation = transformation;
        this.secretKeySpec = secretKeySpec;
        this.ivLength = ivLength;
        this.isGcm = isGcm;
    }

    public int getId() {
        return this.id;
    }

    public AlgorithmParameterSpec createParameterSpec(byte[] iv) {
        if (this.isGcm) {
            return new GCMParameterSpec(128, iv); // 128-bit auth tag
        }
        return new IvParameterSpec(iv);
    }

    public String getTransformation() {
        return this.transformation;
    }

    public int getIvLength() {
        return this.ivLength;
    }

    public String getSecretKeySpec() {
        return this.secretKeySpec;
    }

    public static EncryptionType fromId(int id) {
        for (EncryptionType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        throw new IllegalArgumentException("Not a known algorithm " + id);
    }
}
