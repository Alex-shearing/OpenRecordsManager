package com.openrecordsmanager.plugin.filestoremiddleware_encrypting;

import com.openrecordsmanager.api.filestore.FileStoreMiddlewareType;
import com.openrecordsmanager.api.types.MaskingConverter;
import tools.jackson.databind.annotation.JsonSerialize;

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
import java.util.List;
import java.util.Vector;

public class FilestoreMiddlewareEncryptingType extends FileStoreMiddlewareType<FilestoreMiddlewareEncryptingType.LocalFileStoreSettings> {
    public FilestoreMiddlewareEncryptingType() {
        super(LocalFileStoreSettings.class);
    }

    @Override
    public InputStream duringSave(LocalFileStoreSettings properties, InputStream data) {
        try {
            // Construct the cipher
            Cipher cipher = Cipher.getInstance(properties.algorithm.getTransformation());

            // Construct secret key
            SecretKeySpec secretKey = new SecretKeySpec(properties.secretKey(), properties.algorithm.getSecretKeySpec());

            // Construct nonce
            byte[] iv = new byte[properties.algorithm.getIvLength()];
            SecureRandom.getInstanceStrong().nextBytes(iv);
            AlgorithmParameterSpec paramSpec = properties.algorithm.createParameterSpec(iv);

            // Initialize cipher engine in ENCRYPT mode
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, paramSpec);

            // Returns a concatenated single stream: [ Algorithm Length ] + [ Algorithm ] + [ Nonce Length ] + [ Nonce ] + [ Encrypted Body Bytes ]
            Vector<InputStream> inputStreams = new Vector<>(List.of(
                    new ByteArrayInputStream(new byte[]{(byte) properties.algorithm.name().length()}),
                    new ByteArrayInputStream(properties.algorithm.name().getBytes(StandardCharsets.UTF_8)),
                    new ByteArrayInputStream(new byte[]{(byte) iv.length}),
                    new ByteArrayInputStream(iv),
                    new CipherInputStream(data, cipher)
            ));

            return new SequenceInputStream(inputStreams.elements());
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException |
                 InvalidKeyException e) {
            FileStoreMiddlewareEncryptingPlugin.LOGGER.error("Failed to initialize file encryption", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream duringRetrieve(LocalFileStoreSettings properties, InputStream data) {
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
            SecretKeySpec secretKey = new SecretKeySpec(properties.secretKey(), algorithmName.getSecretKeySpec());

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

    public record LocalFileStoreSettings(@JsonSerialize(converter = MaskingConverter.class) byte[] secretKey,
                                         EncryptionType algorithm) {
    }
}
