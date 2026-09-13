package com.app.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Shared password + email helpers used by the console accounts,
 * the DB login ({@code UserDAO}) and the API auth controller,
 * so all three always agree on hashing and validation.
 * NOTE: SHA-256 is dev-grade; use BCrypt for production.
 */
public final class PasswordUtil {

    public static final int MIN_PASSWORD_LEN = 8;

    private static final String EMAIL_RE = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    private PasswordUtil() {
    }

    public static String sha256(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((raw == null ? "" : raw).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static boolean verify(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null) {
            return false;
        }
        return sha256(rawPassword).equalsIgnoreCase(storedHash.trim());
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
