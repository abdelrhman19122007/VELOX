package com.app.service;

import com.app.dao.UserDAO;
import com.app.dao.WebOrderDAO;
import com.app.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Feature 2: instant wallet compensation for delivered orders.
 * Anti-abuse: DB-unique refund per order_code + owner-only claims.
 */
public class RefundService {

    private final WebOrderDAO lookup = new WebOrderDAO();
    private final UserDAO users = new UserDAO();
    private final NotificationService notifications = new NotificationService();

    public Map<String, Object> claim(String email, String orderCodeOrId, String reason) {
        if (email == null || email.isBlank()) {
            throw new SecurityException("Login required.");
        }
        if (orderCodeOrId == null || orderCodeOrId.isBlank()) {
            throw new IllegalArgumentException("orderId is required.");
        }
        OrderRow order = findOrder(orderCodeOrId.trim());
        if (order == null) {
            throw new IllegalArgumentException("Order not found: " + orderCodeOrId + ".");
        }
        if (!order.userEmail().equalsIgnoreCase(email.trim())) {
            throw new SecurityException("Forbidden.");
        }
        long minutes = Duration.between(order.orderDate(),
                order.deliveredAt() != null ? order.deliveredAt() : LocalDateTime.now()).toMinutes();
        double amount = RefundPolicy.decide(order.status(), minutes, order.total(), reason)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Order is not eligible for instant refund."));
        if (alreadyRefunded(order.code())) {
            throw new IllegalArgumentException("This order was already refunded.");
        }
        if (!users.topUp(order.userId(), amount)) {
            throw new IllegalStateException("Refund failed, please try again.");
        }
        insertRefund(order.code(), order.userId(), amount, reason == null ? "OTHER" : reason.trim().toUpperCase());
        notifications.notify(order.userId(),
                "تم إضافة تعويض " + String.format(java.util.Locale.US, "%.2f", amount) + " EGP",
                "تعويض فوري للطلب " + order.code() + " نزل في محفظتك.",
                "REFUND");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("orderCode", order.code());
        out.put("amount", amount);
        out.put("balance", users.getBalance(order.userId()));
        return out;
    }

    private record OrderRow(int userId, String userEmail, String code,
                            String status, double total,
                            LocalDateTime orderDate, LocalDateTime deliveredAt) {
    }

    private OrderRow findOrder(String codeOrId) {
        String sql = "SELECT o.id, o.order_code, o.user_id, u.email, o.status,"
                + " o.final_amount, o.order_date, d.delivered_at"
                + " FROM orders o JOIN users u ON u.id = o.user_id"
                + " LEFT JOIN deliveries d ON d.order_id = o.id"
                + " WHERE o.id = ? OR o.order_code = ? LIMIT 1";
        int numeric;
        try {
            numeric = Integer.parseInt(codeOrId.trim());
        } catch (NumberFormatException e) {
            numeric = -1;
        }
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(sql)) {
            s.setInt(1, numeric);
            s.setString(2, codeOrId.trim());
            try (ResultSet rs = s.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Timestamp delivered = rs.getTimestamp("delivered_at");
                Timestamp placed = rs.getTimestamp("order_date");
                return new OrderRow(rs.getInt("user_id"), rs.getString("email"),
                        rs.getString("order_code"), rs.getString("status"),
                        rs.getDouble("final_amount"),
                        placed != null ? placed.toLocalDateTime() : LocalDateTime.now(),
                        delivered != null ? delivered.toLocalDateTime() : null);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Refund lookup failed: " + e.getMessage());
        }
    }

    private boolean alreadyRefunded(String orderCode) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(
                     "SELECT 1 FROM refunds WHERE order_code = ? LIMIT 1")) {
            s.setString(1, orderCode);
            try (ResultSet rs = s.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Refund lookup failed: " + e.getMessage());
        }
    }

    private void insertRefund(String orderCode, int userId, double amount, String reason) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(
                     "INSERT INTO refunds (order_code, user_id, amount, reason) VALUES (?, ?, ?, ?)")) {
            s.setString(1, orderCode);
            s.setInt(2, userId);
            s.setDouble(3, amount);
            s.setString(4, reason);
            s.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Refund record failed: " + e.getMessage());
        }
    }
}
