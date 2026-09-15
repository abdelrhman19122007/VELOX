package com.app.service;

import com.app.dao.UserDAO;
import com.app.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Feature 4 (VELOX Plus): monthly subscription — free delivery on every
 * order while active. Pure pricing/expiry rules stay testable; only the
 * activation check and purchase touch the database.
 */
public class PlusService {

    /** Monthly price in EGP (charged from the wallet). */
    public static final double MONTHLY_PRICE = 50.0;
    /** Subscription length in days. */
    public static final int DURATION_DAYS = 30;
    public static final String PLAN = "PLUS_MONTHLY";

    private final UserDAO users = new UserDAO();

    /** Pure rule: subscription counts while expiry is in the future. */
    public static boolean isActive(LocalDateTime expiresAt, LocalDateTime now) {
        return expiresAt != null && now != null && expiresAt.isAfter(now);
    }

    public boolean hasActiveSub(int userId) {
        LocalDateTime exp = expiresAtOf(userId);
        return isActive(exp, LocalDateTime.now());
    }

    public Map<String, Object> statusOf(int userId) {
        LocalDateTime exp = expiresAtOf(userId);
        boolean active = isActive(exp, LocalDateTime.now());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("active", active);
        out.put("plan", PLAN);
        out.put("price", MONTHLY_PRICE);
        out.put("durationDays", DURATION_DAYS);
        out.put("expiresAt", active && exp != null ? exp.toString() : null);
        return out;
    }

    /** Charges the wallet and activates 30 days. Throws 400-style on problems. */
    public Map<String, Object> subscribe(int userId) {
        if (hasActiveSub(userId)) {
            throw new IllegalArgumentException("Subscription is already active.");
        }
        if (!users.debit(userId, MONTHLY_PRICE)) {
            throw new IllegalArgumentException("Insufficient wallet balance (need "
                    + String.format(java.util.Locale.US, "%.2f", MONTHLY_PRICE) + " EGP). Top up first.");
        }
        LocalDateTime expires = LocalDateTime.now().plusDays(DURATION_DAYS);
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(
                     "INSERT INTO subscriptions (user_id, plan, expires_at, status) VALUES (?, ?, ?, 'ACTIVE')")) {
            s.setInt(1, userId);
            s.setString(2, PLAN);
            s.setTimestamp(3, Timestamp.valueOf(expires));
            s.executeUpdate();
        } catch (SQLException e) {
            users.topUp(userId, MONTHLY_PRICE); // give the money back
            throw new IllegalStateException("Subscription failed: " + e.getMessage());
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("plan", PLAN);
        out.put("expiresAt", expires.toString());
        return out;
    }

    private LocalDateTime expiresAtOf(int userId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(
                     "SELECT expires_at FROM subscriptions WHERE user_id = ? AND status = 'ACTIVE'"
                             + " ORDER BY expires_at DESC LIMIT 1")) {
            s.setInt(1, userId);
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp("expires_at");
                    return ts != null ? ts.toLocalDateTime() : null;
                }
            }
        } catch (SQLException e) {
            System.err.println("[PlusService] lookup failed: " + e.getMessage());
        }
        return null;
    }
}
