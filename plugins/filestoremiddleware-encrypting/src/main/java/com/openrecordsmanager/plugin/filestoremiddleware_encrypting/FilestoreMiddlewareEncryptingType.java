package com.openrecordsmanager.plugin.filestoremiddleware_encrypting;

import com.openrecordsmanager.api.filestore.FileStoreMiddlewareType;
import com.openrecordsmanager.api.types.MaskingConverter;
import tools.jackson.databind.annotation.JsonSerialize;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class FilestoreMiddlewareEncryptingType extends FileStoreMiddlewareType<FilestoreMiddlewareEncryptingType.LocalFileStoreSettings> {

    private static final String ALGORITHM = "ChaCha20-Poly1305";
    private static final int NONCE_SIZE_BYTES = 12; // Standard 96-bit nonce for ChaCha20

    public FilestoreMiddlewareEncryptingType() {
        super(LocalFileStoreSettings.class);
    }

    @Override
    public InputStream duringSave(LocalFileStoreSettings properties, InputStream data) {
        try {
            // Construct secret key
            SecretKeySpec secretKey = new SecretKeySpec(properties.secretKey(), "ChaCha20");

            // Construct nonce
            byte[] nonce = new byte[NONCE_SIZE_BYTES];
            SecureRandom.getInstanceStrong().nextBytes(nonce);
            IvParameterSpec ivSpec = new IvParameterSpec(nonce);

            // Initialize cipher engine in ENCRYPT mode
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

            // Returns a concatenated single stream: [ 12-byte Nonce ] + [ Encrypted Body Bytes ]
            return new SequenceInputStream(
                    new ByteArrayInputStream(nonce),
                    new CipherInputStream(data, cipher)
            );
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException |
                 InvalidKeyException e) {
            FileStoreMiddlewareEncryptingPlugin.LOGGER.error("Failed to initialize file encryption", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream duringRetrieve(LocalFileStoreSettings properties, InputStream data) {
        try {
            // Extract the 12-byte nonce prefixed at the front of the database stream
            byte[] nonce = new byte[NONCE_SIZE_BYTES];
            int bytesRead = data.readNBytes(nonce, 0, NONCE_SIZE_BYTES);

            // Validate that the stream isn't truncated or empty
            if (bytesRead < NONCE_SIZE_BYTES) {
                throw new RuntimeException("Corrupted stream payload: Failed to extract a valid encryption nonce header.");
            }

            // Construct secret key
            SecretKeySpec secretKey = new SecretKeySpec(properties.secretKey(), "ChaCha20");

            // Initialize cipher engine in DECRYPT mode
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(nonce));

            return new CipherInputStream(data, cipher);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException |
                 InvalidKeyException | IOException e) {
            FileStoreMiddlewareEncryptingPlugin.LOGGER.error("Failed to initialize file decryption", e);
            throw new RuntimeException(e);
        }
    }

    public record LocalFileStoreSettings(@JsonSerialize(converter = MaskingConverter.class) byte[] secretKey) {
    }
}
