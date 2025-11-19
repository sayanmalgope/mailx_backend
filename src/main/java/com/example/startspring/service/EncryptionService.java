package com.example.startspring.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Service
public class EncryptionService {

    @Value("${encryption.secret-key}")
    private String secretKey;

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    // Decrypts strings in the format "iv_base64:ciphertext_base64".
    // If input doesn't contain ":", we assume it's plaintext and return it (with a warning).
    public String decrypt(String encryptedString) throws Exception {
        if (encryptedString == null || encryptedString.isEmpty()) {
            throw new IllegalArgumentException("Encrypted string is null/empty");
        }

        // Fallback: if no ":" then likely plaintext (frontend didn't encrypt with server key)
        if (!encryptedString.contains(":")) {
            // WARNING: plaintext detected — this is insecure and should be removed in production.
            // Returning plaintext so processing can continue while you migrate to a secure approach.
            return encryptedString;
        }

        String[] parts = encryptedString.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid encrypted string format. Expected 'iv:ciphertext'.");
        }

        byte[] iv = Base64.getDecoder().decode(parts[0]);
        byte[] cipherText = Base64.getDecoder().decode(parts[1]);

        // Derive 32-byte key from configured secret using SHA-256 (stable length)
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha.digest(secretKey.getBytes(StandardCharsets.UTF_8));
        byte[] aesKey = Arrays.copyOf(keyBytes, 32);

        SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        byte[] decryptedBytes = cipher.doFinal(cipherText);

        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    // Optional: server-side helper to create "iv:ciphertext" strings if you want backend encryption
    public String encrypt(String plaintext) throws Exception {
        if (plaintext == null) plaintext = "";
        SecureRandom secureRandom = new SecureRandom();
        byte[] iv = new byte[16];
        secureRandom.nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha.digest(secretKey.getBytes(StandardCharsets.UTF_8));
        byte[] aesKey = Arrays.copyOf(keyBytes, 32);
        SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        return Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(encrypted);
    }
}
