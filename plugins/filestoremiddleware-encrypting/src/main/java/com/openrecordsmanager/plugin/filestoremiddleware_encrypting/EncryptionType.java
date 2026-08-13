package com.openrecordsmanager.plugin.filestoremiddleware_encrypting;

import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import java.security.spec.AlgorithmParameterSpec;

public enum EncryptionType {
    AES("AES/GCM/NoPadding", "AES", 12, true);

    private final String transformation;
    private final String secretKeySpec;
    private final int ivLength;
    private final boolean isGcm;

    EncryptionType(String transformation, String secretKeySpec, int ivLength, boolean isGcm) {
        this.transformation = transformation;
        this.secretKeySpec = secretKeySpec;
        this.ivLength = ivLength;
        this.isGcm = isGcm;
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
}
