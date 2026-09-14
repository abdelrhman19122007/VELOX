package com.app.service;

import com.app.util.DatabaseConnection;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Persistent user notifications (MySQL user_notifications).
 * Emitted automatically on order/complaint/return events.
 */
@Service
public class NotificationService {

    public int notify(int userId, String title, String message, String type) {
        String sql = "INSERT INTO user_notifications (user_id, title, message, type) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setInt(1, userId);
            s.setString(2, title);
            s.setString(3, message);
            s.setString(4, type == null ? "GENERAL" : type);
            s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[Notifications] insert failed: " + e.getMessage());
        }
        return -1;
    }

    public int notifyEmail(String email, String title, String message, String type) {
        Integer id = userIdOf(email);
        if (id == null) {
            return -1;
        }
        return notify(id, title, message, type);
    }

    public List<Map<String, Object>> list(int userId, int limit) {
        List<Map<String, Object>> out = new ArrayList<>();
        String sql = "SELECT id, title, message, type, is_read, created_at FROM user_notifications"
                + " WHERE user_id = ? ORDER BY id DESC LIMIT ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(sql)) {
            s.setInt(1, userId);
            s.setInt(2, Math.min(Math.max(limit, 1), 100));
            try (ResultSet rs = s.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getInt("id"));
                    m.put("title", rs.getString("title"));
                    m.put("message", rs.getString("message"));
                    m.put("type", rs.getString("type"));
                    m.put("is_read", rs.getInt("is_read") == 1);
                    m.put("created_at", rs.getTimestamp("created_at").toLocalDateTime().toString());
                    out.add(m);
                }
            }
        } catch (SQLException e) {
            System.err.println("[Notifications] list failed: " + e.getMessage());
        }
        return out;
    }

    public int markAllRead(int userId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(
                     "UPDATE user_notifications SET is_read = 1 WHERE user_id = ? AND is_read = 0")) {
            s.setInt(1, userId);
            return s.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[Notifications] mark-read failed: " + e.getMessage());
            return 0;
        }
    }

    private Integer userIdOf(String email) {
        if (email == null) {
            return null;
        }
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(
                     "SELECT id FROM users WHERE email = ? LIMIT 1")) {
            s.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[Notifications] user lookup failed: " + e.getMessage());
        }
        return null;
    }
}
