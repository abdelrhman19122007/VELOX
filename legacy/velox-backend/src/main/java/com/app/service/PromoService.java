package com.app.service;

import com.app.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Coupon codes (CASHBACK25, SHW-FREE, VELOX50, ...). Pure validation rules
 * stay unit-testable; DB access is isolated in small methods.
 */
public class PromoService {

    public record Coupon(String code, double percent, double minOrder,
                         Integer maxUses, int usedCount, boolean freeShipping,
                         LocalDateTime expiresAt) {
    }

    public record Applied(double discount, boolean freeShipping) {
    }

    /** Pure rule: is this coupon usable for the given subtotal right now? */
    public static Optional<String> rejectionReason(Coupon c, double subtotal, LocalDateTime now) {
        if (c == null) {
            return Optional.of("Invalid coupon code.");
        }
        if (c.expiresAt() != null && !c.expiresAt().isAfter(now)) {
            return Optional.of("Coupon expired.");
        }
        if (c.maxUses() != null && c.usedCount() >= c.maxUses()) {
            return Optional.of("Coupon fully redeemed.");
        }
        if (subtotal < c.minOrder()) {
            return Optional.of("Order must be at least "
                    + String.format(java.util.Locale.US, "%.0f", c.minOrder()) + " EGP for this coupon.");
        }
        return Optional.empty();
    }

    /** Pure math: discount for a valid coupon (free-shipping handled by caller). */
    public static Applied apply(Coupon c, double subtotal) {
        double discount = Math.min(subtotal * c.percent() / 100.0, subtotal);
        return new Applied(discount, c.freeShipping());
    }

    public Coupon find(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String sql = "SELECT code, discount_percentage, min_order_amount, max_uses,"
                + " used_count, free_shipping, expires_at FROM promo_codes"
                + " WHERE UPPER(code) = UPPER(?) AND is_active = 1 LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(sql)) {
            s.setString(1, code.trim());
            try (ResultSet rs = s.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                java.sql.Timestamp exp = rs.getTimestamp("expires_at");
                Object maxUses = rs.getObject("max_uses");
                return new Coupon(
                        rs.getString("code"),
                        rs.getDouble("discount_percentage"),
                        rs.getDouble("min_order_amount"),
                        maxUses == null ? null : ((Number) maxUses).intValue(),
                        rs.getInt("used_count"),
                        rs.getInt("free_shipping") == 1,
                        exp != null ? exp.toLocalDateTime() : null);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Coupon lookup failed: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> activeCoupons() {
        List<Map<String, Object>> out = new ArrayList<>();
        String sql = "SELECT code, description, discount_percentage, min_order_amount,"
                + " free_shipping, expires_at FROM promo_codes WHERE is_active = 1"
                + " AND (expires_at IS NULL OR expires_at > NOW())"
                + " AND (max_uses IS NULL OR used_count < max_uses)"
                + " ORDER BY discount_percentage DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(sql);
             ResultSet rs = s.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("code", rs.getString("code"));
                row.put("description", rs.getString("description"));
                row.put("discount", rs.getDouble("discount_percentage"));
                row.put("minOrder", rs.getDouble("min_order_amount"));
                row.put("freeShipping", rs.getInt("free_shipping") == 1);
                out.add(row);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Coupons unavailable: " + e.getMessage());
        }
        return out;
    }

    public void markUsed(String code) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(
                     "UPDATE promo_codes SET used_count = used_count + 1 WHERE UPPER(code) = UPPER(?)")) {
            s.setString(1, code.trim());
            s.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[PromoService] use-count failed: " + e.getMessage());
        }
    }
}
