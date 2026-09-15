package com.openrecordsmanager.plugin.filestoremiddleware_encrypting;

import com.openrecordsmanager.api.errors.InputValidationException;
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
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class FilestoreMiddlewareEncryptingType extends FileStoreMiddlewareType<FilestoreMiddlewareEncryptingType.EncryptingMiddlewareSettings> {
    public FilestoreMiddlewareEncryptingType() {
        super(EncryptingMiddlewareSettings.class);
    }

    @Override
    public void initialize(EncryptingMiddlewareSettings settings) {
        try {
            Cipher cipher = Cipher.getInstance(settings.algorithm().getTransformation());
            SecretKeySpec secretKey = new SecretKeySpec(settings.secretKey(), settings.algorithm().getSecretKeySpec());
            byte[] iv = new byte[settings.algorithm().getIvLength()];
            SecureRandom.getInstanceStrong().nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, settings.algorithm().createParameterSpec(iv));
        } catch (InvalidKeyException e) {
            throw new InputValidationException(Map.of(
                    "secretKey",
                    "Invalid secret key for " + settings.algorithm().name() + ": " + e.getMessage()
            ));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new InputValidationException(Map.of(
                    "algorithm",
                    "Unsupported algorithm " + settings.algorithm().name() + ": " + e.getMessage()
            ));
        } catch (InvalidAlgorithmParameterException e) {
            throw new InputValidationException(Map.of(
                    "algorithm",
                    "Invalid algorithm parameters for " + settings.algorithm().name() + ": " + e.getMessage()
            ));
        }
    }

    @Override
    public InputStream duringSave(EncryptingMiddlewareSettings settings, InputStream data) {
        try {
            // Construct the cipher
            Cipher cipher = Cipher.getInstance(settings.algorithm().getTransformation());

            // Construct secret key
            SecretKeySpec secretKey = new SecretKeySpec(settings.secretKey(), settings.algorithm().getSecretKeySpec());

            // Construct nonce
            byte[] iv = new byte[settings.algorithm().getIvLength()];
            SecureRandom.getInstanceStrong().nextBytes(iv);
            AlgorithmParameterSpec paramSpec = settings.algorithm().createParameterSpec(iv);

            // Initialize cipher engine in ENCRYPT mode
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, paramSpec);

            // Returns a concatenated single stream: [ Algorithm Length ] + [ Algorithm ] + [ Nonce Length ] + [ Nonce ] + [ Encrypted Body Bytes ]
            return new SequenceInputStream(Collections.enumeration(List.of(
                    new ByteArrayInputStream(new byte[]{(byte) settings.algorithm().name().length()}),
                    new ByteArrayInputStream(settings.algorithm().name().getBytes(StandardCharsets.UTF_8)),
                    new ByteArrayInputStream(new byte[]{(byte) iv.length}),
                    new ByteArrayInputStream(iv),
                    new CipherInputStream(data, cipher)
            )));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException |
                 InvalidKeyException e) {
            FileStoreMiddlewareEncryptingPlugin.LOGGER.error("Failed to initialize file encryption", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream duringRetrieve(EncryptingMiddlewareSettings settings, InputStream data) {
        try {
            // Read the algorithm length
            int algoLength = data.read();
            if (algoLength == -1) {
                throw new IOException("Corrupted stream payload: Failed to read algorithm length header.");
            }

            // Read the algorithm name string
            byte[] algoBytes = new byte[algoLength];
            int algoBytesRead = data.readNBytes(algoBytes, 0, algoLength);
            if (algoBytesRead != algoLength) {
                throw new IOException("Corrupted stream payload: Failed to extract a valid algorithm name.");
            }
            EncryptionType algorithmName = EncryptionType.valueOf(new String(algoBytes, StandardCharsets.UTF_8));

            // Read the nonce length
            int ivLength = data.read();
            if (ivLength == -1) {
                throw new IOException("Corrupted stream payload: Failed to read nonce length header.");
            }

            byte[] iv = new byte[ivLength];
            int nonceBytesRead = data.readNBytes(iv, 0, ivLength);
            if (nonceBytesRead != ivLength) {
                throw new IOException("Corrupted stream payload: Failed to extract a valid nonce.");
            }

            // Construct secret key
            SecretKeySpec secretKey = new SecretKeySpec(settings.secretKey(), algorithmName.getSecretKeySpec());

            // Initialize cipher engine in DECRYPT mode
            Cipher cipher = Cipher.getInstance(algorithmName.getTransformation());
            cipher.init(Cipher.DECRYPT_MODE, secretKey, algorithmName.createParameterSpec(iv));

            return new CipherInputStream(data, cipher);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException |
                 InvalidKeyException | IOException e) {
            FileStoreMiddlewareEncryptingPlugin.LOGGER.error("Failed to initialize file decryption", e);
            throw new RuntimeException(e);
        }
    }

    public record EncryptingMiddlewareSettings(
            @Schema(
                    title = "Secret Key",
                    type = "string",
                    format = "byte",
                    accessMode = Schema.AccessMode.WRITE_ONLY
            )
            byte[] secretKey,
            @Schema(title = "Algorithm") EncryptionType algorithm
    ) {
    }
}
