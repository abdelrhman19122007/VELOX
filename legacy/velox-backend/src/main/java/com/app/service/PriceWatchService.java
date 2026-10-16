package com.app.service;

import com.app.dao.WebOrderDAO;
import com.app.util.DatabaseConnection;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Feature 5: watch a product and get notified on price drops.
 * A scheduled sweep (hourly) compares live prices against targets.
 */
@Service
public class PriceWatchService {

    private final WebOrderDAO lookup = new WebOrderDAO();
    private final NotificationService notifications = new NotificationService();

    public Map<String, Object> watch(String email, int productId, Double targetPrice) {
        Integer userId = lookup.findUserId(email);
        if (userId == null) {
            throw new IllegalArgumentException("Account not found.");
        }
        double target = targetPrice != null ? targetPrice : -1;
        Double current = currentPrice(productId);
        if (current == null) {
            throw new IllegalArgumentException("Product not found: " + productId + ".");
        }
        if (target <= 0) {
            target = current; // any drop below today's price triggers
        }
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(
                     "INSERT INTO price_watches (user_id, product_id, target_price)"
                             + " VALUES (?, ?, ?)"
                             + " ON DUPLICATE KEY UPDATE target_price = VALUES(target_price), notified = 0")) {
            s.setInt(1, userId);
            s.setInt(2, productId);
            s.setDouble(3, target);
            s.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save watch: " + e.getMessage());
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("productId", productId);
        out.put("targetPrice", target);
        out.put("currentPrice", current);
        return out;
    }

    public List<Map<String, Object>> list(String email) {
        Integer userId = lookup.findUserId(email);
        List<Map<String, Object>> out = new ArrayList<>();
        if (userId == null) {
            return out;
        }
        String sql = "SELECT w.product_id, w.target_price, w.notified, p.name, p.price"
                + " FROM price_watches w JOIN products p ON p.id = w.product_id"
                + " WHERE w.user_id = ? ORDER BY w.created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(sql)) {
            s.setInt(1, userId);
            try (ResultSet rs = s.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("productId", rs.getInt("product_id"));
                    row.put("name", rs.getString("name"));
                    row.put("targetPrice", rs.getDouble("target_price"));
                    row.put("currentPrice", rs.getDouble("price"));
                    row.put("notified", rs.getInt("notified") == 1);
                    out.add(row);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not list watches: " + e.getMessage());
        }
        return out;
    }

    public void unwatch(String email, int productId) {
        Integer userId = lookup.findUserId(email);
        if (userId == null) {
            throw new IllegalArgumentException("Account not found.");
        }
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(
                     "DELETE FROM price_watches WHERE user_id = ? AND product_id = ?")) {
            s.setInt(1, userId);
            s.setInt(2, productId);
            if (s.executeUpdate() != 1) {
                throw new IllegalArgumentException("No watch for product " + productId + ".");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not remove watch: " + e.getMessage());
        }
    }

    /** Hourly sweep: fire newly-dropped watches, re-arm recovered ones. */
    @Scheduled(fixedDelay = 3600000)
    public void sweep() {
        String sql = "SELECT w.id, w.user_id, w.product_id, w.target_price, w.notified,"
                + " p.name, p.price FROM price_watches w"
                + " JOIN products p ON p.id = w.product_id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(sql);
             ResultSet rs = s.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                boolean notified = rs.getInt("notified") == 1;
                double target = rs.getDouble("target_price");
                double current = rs.getDouble("price");
                if (PriceWatchMatcher.shouldNotify(current, target, notified)) {
                    markNotified(id, true);
                    notifications.notify(rs.getInt("user_id"),
                            "سعر " + rs.getString("name") + " نزل!",
                            "السعر الحالي " + current + " EGP (كنت بتراقب " + target + " EGP).",
                            "PRICE_DROP");
                } else if (PriceWatchMatcher.shouldRearm(current, target, notified)) {
                    markNotified(id, false);
                }
            }
        } catch (SQLException e) {
            System.err.println("[PriceWatch] sweep failed: " + e.getMessage());
        }
    }

    private Double currentPrice(int productId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(
                     "SELECT price FROM products WHERE id = ? AND is_available = 1")) {
            s.setInt(1, productId);
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("price");
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Price lookup failed: " + e.getMessage());
        }
        return null;
    }

    private void markNotified(int watchId, boolean value) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(
                     "UPDATE price_watches SET notified = ? WHERE id = ?")) {
            s.setInt(1, value ? 1 : 0);
            s.setInt(2, watchId);
            s.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[PriceWatch] mark failed: " + e.getMessage());
        }
    }
}
