package com.openrecordsmanager.plugin.filestoremiddleware_encrypting;

import com.openrecordsmanager.api.errors.ApiException;
import com.openrecordsmanager.api.filestore.FileStoreMiddlewareType;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class FilestoreMiddlewareEncryptingType extends FileStoreMiddlewareType<FilestoreMiddlewareEncryptingType.EncryptingMiddlewareSettings> {
    public FilestoreMiddlewareEncryptingType() {
        super(EncryptingMiddlewareSettings.class);
    }

    @Override
    public void initialize(EncryptingMiddlewareSettings settings) {
        try {
            settings.newEncryptCipher();
        } catch (InvalidKeyException e) {
            throw ApiException.validationFailed("secretKey", "encrypting_middleware_key_invalid", e.getMessage());
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException e) {
            throw ApiException.validationFailed("algorithm", "encrypting_middleware_algorithm_required", e.getMessage());
        }
    }

    @Override
    public InputStream duringSave(EncryptingMiddlewareSettings settings, InputStream data) {
        try {
            Cipher cipher = settings.newEncryptCipher();
            byte[] iv = cipher.getIV();

            // Header: [ Algorithm ] + [ Nonce Length ] + [ Nonce ]
            byte[] header = ByteBuffer.allocate(1 + 1 + iv.length)
                    .put((byte) settings.algorithm().getId())
                    .put((byte) iv.length).put(iv)
                    .array();

            return new SequenceInputStream(
                    new ByteArrayInputStream(header),
                    new CipherInputStream(data, cipher)
            );
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException |
                 InvalidKeyException e) {
            FileStoreMiddlewareEncryptingPlugin.LOGGER.error("Failed to initialize file encryption", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream duringRetrieve(EncryptingMiddlewareSettings settings, InputStream data) {
        try {
            // Read the algorithm
            int algoId = data.read();
            if (algoId == -1) {
                throw new IOException("Corrupted stream payload: Failed to read algorithm header.");
            }
            EncryptionType alg = EncryptionType.fromId(algoId);

            // Read the nonce length
            int ivLength = data.read();
            if (ivLength == -1) {
                throw new IOException("Corrupted stream payload: Failed to read nonce length header.");
            }

            byte[] iv = data.readNBytes(ivLength);
            if (iv.length != ivLength) {
                throw new IOException("Corrupted stream payload: Failed to extract a valid nonce.");
            }

            return new CipherInputStream(data, settings.createCipher(Cipher.DECRYPT_MODE, alg, iv));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException |
                 InvalidKeyException | IOException e) {
            FileStoreMiddlewareEncryptingPlugin.LOGGER.error("Failed to initialize file decryption", e);
            throw new RuntimeException(e);
        }
    }

    public record EncryptingMiddlewareSettings(
            @Schema(
                    title = "file_store_middleware.filestoremiddleware_encrypting.encrypting.schema.secretKey.title",
                    type = "string",
                    format = "byte",
                    accessMode = Schema.AccessMode.WRITE_ONLY
            )
            byte[] secretKey,
            @Schema(title = "file_store_middleware.filestoremiddleware_encrypting.encrypting.schema.algorithm.title")
            EncryptionType algorithm
    ) {
        private Cipher newEncryptCipher()
                throws NoSuchAlgorithmException, NoSuchPaddingException,
                InvalidKeyException, InvalidAlgorithmParameterException {
            byte[] iv = new byte[algorithm.getIvLength()];
            SecureRandom.getInstanceStrong().nextBytes(iv);

            return this.createCipher(Cipher.ENCRYPT_MODE, this.algorithm, iv);
        }

        private Cipher createCipher(int opmode, EncryptionType algorithm, byte[] iv)
                throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
                InvalidAlgorithmParameterException {
            Cipher cipher = Cipher.getInstance(algorithm.getTransformation());
            SecretKeySpec spec = new SecretKeySpec(this.secretKey, algorithm.getSecretKeySpec());

            cipher.init(opmode, spec, algorithm.createParameterSpec(iv));
            return cipher;
        }
    }
}
