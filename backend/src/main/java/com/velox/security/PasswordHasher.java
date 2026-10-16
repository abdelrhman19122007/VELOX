package com.velox.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.springframework.stereotype.Component;

/**
 * PBKDF2-HMAC-SHA256 with a random 16-byte salt per password, stored as {@code pbkdf2$rounds$salt$hash}
 * (OWASP A02 — 210 000 rounds is the OWASP 2023 recommendation). Comparison is constant-time.
 */
@Component
public class PasswordHasher {

    static final int ROUNDS = 210_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    public String hash(String password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return "pbkdf2$" + ROUNDS + "$" + b64(salt) + "$" + b64(pbkdf2(password, salt, ROUNDS));
    }

    public boolean matches(String password, String stored) {
        if (password == null || stored == null) {
            return false;
        }
        String[] parts = stored.split("\\$");
        if (parts.length != 4 || !"pbkdf2".equals(parts[0])) {
            return false;
        }
        byte[] expected = Base64.getDecoder().decode(parts[3]);
        byte[] actual = pbkdf2(password, Base64.getDecoder().decode(parts[2]), Integer.parseInt(parts[1]));
        return MessageDigest.isEqual(expected, actual);
    }

    /** Short random secret people can type (temporary passwords): no 0/O/1/l confusion. */
    public String randomReadable(int length) {
        String alphabet = "abcdefghjkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        }
        return sb.toString();
    }

    private static byte[] pbkdf2(String password, byte[] salt, int rounds) {
        try {
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, rounds, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("PBKDF2 unavailable", e);
        }
    }

    private static String b64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
