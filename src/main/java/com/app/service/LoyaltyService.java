package com.app.service;

import com.app.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Loyalty program backed by MySQL: every {@value #ORDERS_PER_REWARD}
 * successful orders (DELIVERED/PAID, returns excluded by status) earns one
 * FREE delivery. Consumed rewards persist in
 * {@code users.loyalty_rewards_consumed}, so they survive restarts.
 */
public final class LoyaltyService {

    public static final int ORDERS_PER_REWARD = 5;

    private LoyaltyService() {
    }

    public static int successfulCount(String email) {
        Integer dbId = userIdOf(email);
        if (dbId == null) {
            return 0;
        }
        String sql = "SELECT COUNT(*) FROM orders WHERE user_id = ?"
                + " AND status IN ('DELIVERED','PAID') AND COALESCE(is_returned, 0) = 0";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(sql)) {
            s.setInt(1, dbId);
            try (ResultSet rs = s.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[Loyalty] count failed: " + e.getMessage());
            return 0;
        }
    }

    public static int rewardsEarned(int successful) {
        return successful / ORDERS_PER_REWARD;
    }

    public static int rewardsConsumed(String email) {
        Integer dbId = userIdOf(email);
        if (dbId == null) {
            return 0;
        }
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(
                     "SELECT loyalty_rewards_consumed FROM users WHERE id = ?")) {
            s.setInt(1, dbId);
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[Loyalty] consumed read failed: " + e.getMessage());
        }
        return 0;
    }

    public static boolean isRewardAvailable(String email) {
        return rewardsEarned(successfulCount(email)) > rewardsConsumed(email);
    }

    /** Progress inside the current 5-order cycle, e.g. 3/5. */
    public static int progressInCycle(String email) {
        return successfulCount(email) % ORDERS_PER_REWARD;
    }

    public static void consumeReward(String email) {
        Integer dbId = userIdOf(email);
        if (dbId == null) {
            return;
        }
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(
                     "UPDATE users SET loyalty_rewards_consumed = loyalty_rewards_consumed + 1 WHERE id = ?")) {
            s.setInt(1, dbId);
            s.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[Loyalty] consume failed: " + e.getMessage());
        }
    }

    public static String progressMessage(String email) {
        int ok = successfulCount(email);
        if (isRewardAvailable(email)) {
            return "Loyalty: you earned FREE delivery! (" + ok + " successful orders)";
        }
        int need = ORDERS_PER_REWARD - (ok % ORDERS_PER_REWARD);
        return "Loyalty: " + (ok % ORDERS_PER_REWARD) + "/" + ORDERS_PER_REWARD
                + " - " + need + " more successful order(s) for FREE delivery (total: " + ok + ")";
    }

    private static Integer userIdOf(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            return null;
        }
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(
                     "SELECT id FROM users WHERE email = ? LIMIT 1")) {
            s.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            System.err.println("[Loyalty] user lookup failed: " + e.getMessage());
        }
        return null;
    }
}
