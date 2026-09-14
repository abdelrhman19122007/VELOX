package com.app.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Shared password helpers: BCrypt for all new hashes, transparent fallback
 * to the legacy hex-SHA-256 hashes created before the migration (those are
 * upgraded to BCrypt on the next successful login).
 */
public final class PasswordUtil {

    public static final int MIN_PASSWORD_LEN = 8;

    private static final String EMAIL_RE = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();

    private PasswordUtil() {
    }

    public static String encode(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Password is required.");
        }
        return BCRYPT.encode(rawPassword);
    }

    public static boolean isBcryptHash(String stored) {
        return stored != null
                && (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$"));
    }

    public static boolean verify(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null) {
            return false;
        }
        String stored = storedHash.trim();
        if (isBcryptHash(stored)) {
            try {
                return BCRYPT.matches(rawPassword, stored);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return sha256(rawPassword).equalsIgnoreCase(stored);
    }

    /** True when a legacy (non-BCrypt) hash should be upgraded after login. */
    public static boolean needsRehash(String storedHash) {
        return storedHash != null && !isBcryptHash(storedHash.trim());
    }

    public static String sha256(String raw) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((raw == null ? "" : raw).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static boolean isValidEmail(String email) {
        return email != null && email.trim().matches(EMAIL_RE);
    }

    /** Throws IllegalArgumentException if the password is too weak. */
    public static void requireStrong(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LEN) {
            throw new IllegalArgumentException(
                    "Password must be at least " + MIN_PASSWORD_LEN + " characters.");
        }
    }
}
