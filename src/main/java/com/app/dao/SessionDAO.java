package com.app.dao;

import com.app.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Persistent token store (MySQL {@code sessions} table) replacing the old
 * in-memory map: sessions survive restarts and can be revoked.
 */
public class SessionDAO {

    public String create(String email, long ttlMs) {
        cleanup();
        String token = java.util.UUID.randomUUID().toString().replace("-", "")
                + java.util.UUID.randomUUID().toString().replace("-", "");
        String sql = "INSERT INTO sessions (token, email, expires_at) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(sql)) {
            s.setString(1, token);
            s.setString(2, email);
            s.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now().plusNanos(ttlMs * 1_000_000)));
            s.executeUpdate();
            return token;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not create session: " + e.getMessage());
        }
    }

    public String resolve(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        cleanup();
        String sql = "SELECT email FROM sessions WHERE token = ? AND expires_at > NOW()";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(sql)) {
            s.setString(1, token.trim());
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("email");
                }
            }
        } catch (SQLException e) {
            System.err.println("[SessionDAO] resolve failed: " + e.getMessage());
        }
        return null;
    }

    public void revoke(String token) {
        if (token == null) {
            return;
        }
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement("DELETE FROM sessions WHERE token = ?")) {
            s.setString(1, token.trim());
            s.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[SessionDAO] revoke failed: " + e.getMessage());
        }
    }

    private void cleanup() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement("DELETE FROM sessions WHERE expires_at <= NOW()")) {
            s.executeUpdate();
        } catch (SQLException ignored) {
        }
    }
}
