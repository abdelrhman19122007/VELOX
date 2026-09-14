package com.app.service;

import com.app.util.DatabaseConnection;
import com.app.util.PasswordUtil;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Registration OTP codes (6 digits, 10-minute TTL, max 5 attempts).
 * Codes are stored hashed; the plain code is returned to the caller once
 * (dev-mode: shown on screen / API response — production must SMS it).
 */
public final class OtpService {

    public static final int CODE_DIGITS = 6;
    public static final long TTL_MINUTES = 10;
    public static final int MAX_ATTEMPTS = 5;

    private static final SecureRandom RANDOM = new SecureRandom();

    private OtpService() {
    }

    /** Issues a fresh code (expiring previous unused ones). Returns the plain code. */
    public static String issue(String email) {
        String clean = email.trim().toLowerCase();
        StringBuilder sb = new StringBuilder(CODE_DIGITS);
        for (int i = 0; i < CODE_DIGITS; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        String code = sb.toString();
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement s = conn.prepareStatement(
                    "UPDATE otp_codes SET used = 1 WHERE email = ? AND used = 0")) {
                s.setString(1, clean);
                s.executeUpdate();
            }
            try (PreparedStatement s = conn.prepareStatement(
                    "INSERT INTO otp_codes (email, code_hash, expires_at) VALUES (?, ?, NOW() + INTERVAL 10 MINUTE)",
                    Statement.RETURN_GENERATED_KEYS)) {
                s.setString(1, clean);
                s.setString(2, PasswordUtil.sha256(code));
                s.executeUpdate();
            }
            return code;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not issue code: " + e.getMessage());
        }
    }

    /** Verifies a code; throws IllegalArgumentException on any failure. */
    public static void verify(String email, String code) {
        String clean = email == null ? "" : email.trim().toLowerCase();
        String attempt = code == null ? "" : code.trim();
        String sql = "SELECT id, code_hash, expires_at, attempts, used FROM otp_codes"
                + " WHERE email = ? ORDER BY id DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(sql)) {
            s.setString(1, clean);
            try (ResultSet rs = s.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Invalid or expired code.");
                }
                int id = rs.getInt("id");
                boolean ok = rs.getInt("used") == 0
                        && rs.getTimestamp("expires_at").toLocalDateTime()
                                .isAfter(java.time.LocalDateTime.now().minusSeconds(1))
                        && rs.getInt("attempts") < MAX_ATTEMPTS
                        && PasswordUtil.sha256(attempt).equalsIgnoreCase(rs.getString("code_hash"));
                if (!ok) {
                    try (PreparedStatement u = conn.prepareStatement(
                            "UPDATE otp_codes SET attempts = attempts + 1 WHERE id = ?")) {
                        u.setInt(1, id);
                        u.executeUpdate();
                    }
                    throw new IllegalArgumentException("Invalid or expired code.");
                }
                try (PreparedStatement u = conn.prepareStatement(
                        "UPDATE otp_codes SET used = 1 WHERE id = ?")) {
                    u.setInt(1, id);
                    u.executeUpdate();
                }
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (SQLException e) {
            throw new IllegalStateException("Verification failed: " + e.getMessage());
        }
    }
}
