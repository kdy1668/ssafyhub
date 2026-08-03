package com.ssafyhub.backend.global.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SecretEncryptor {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String VERSION = "v1";
    private static final int KEY_LENGTH_BYTES = 32;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public SecretEncryptor(@Value("${security.encryption-key}") String encodedKey) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(encodedKey);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("SECRET_ENCRYPTION_KEY must be valid Base64.", exception);
        }

        if (keyBytes.length != KEY_LENGTH_BYTES) {
            throw new IllegalStateException("SECRET_ENCRYPTION_KEY must decode to exactly 32 bytes.");
        }

        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plainText) {
        byte[] iv = new byte[IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            return "%s:%s:%s".formatted(
                    VERSION,
                    Base64.getEncoder().encodeToString(iv),
                    Base64.getEncoder().encodeToString(encrypted)
            );
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to encrypt secret value.", exception);
        }
    }

    /** encrypt()로 저장한 "v1:{iv}:{암호문}" 형식의 값을 원문으로 복호화한다. */
    public String decrypt(String encoded) {
        String[] parts = encoded.split(":", 3);
        if (parts.length != 3 || !VERSION.equals(parts[0])) {
            throw new IllegalStateException("Unsupported encrypted value format.");
        }
        try {
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] cipherText = Base64.getDecoder().decode(parts[2]);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to decrypt secret value.", exception);
        }
    }
}
