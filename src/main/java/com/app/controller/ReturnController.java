package com.app.controller;

import com.app.service.AuthTokenStore;
import com.app.service.NotificationService;
import com.app.service.OrderService;
import com.app.util.DatabaseConnection;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Return requests. Web returns go through a pipeline
 * (REQUESTED -> UNDER_REVIEW -> APPROVED -> REFUND_PROCESSED) advanced by
 * the delivery scheduler with a notification at every stage.
 * Eligible: own DELIVERED order within 14 days, no previous request.
 *
 * POST /api/returns {orderId}
 * GET  /api/returns?userId=
 */
@RestController
@RequestMapping("/api/returns")
public class ReturnController {

    private final OrderService orders = new OrderService();
    private final NotificationService notifications = new NotificationService();

    @PostMapping
    public ResponseEntity<?> requestReturn(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        String email = AuthTokenStore.resolve(authorization);
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Login required."));
        }
        Object oid = body.get("orderId") != null ? body.get("orderId") : body.get("order_id");
        if (oid == null || String.valueOf(oid).isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "orderId is required."));
        }
        String code = String.valueOf(oid).trim();
        try {
            orders.getTracking(code);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Order not found."));
        }
        if (!orders.ownsOrder(code, email)) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden."));
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            int orderId;
            String status;
            LocalDateTime orderDate;
            int userId;
            try (PreparedStatement s = conn.prepareStatement(
                    "SELECT o.id, o.status, o.order_date, o.user_id FROM orders o"
                            + " JOIN users u ON u.id = o.user_id WHERE (o.id = ? OR o.order_code = ?)"
                            + " AND u.email = ? LIMIT 1")) {
                int numeric = toIntOr(code, -1);
                s.setInt(1, numeric);
                s.setString(2, code);
                s.setString(3, email.toLowerCase());
                try (ResultSet rs = s.executeQuery()) {
                    if (!rs.next()) {
                        return ResponseEntity.status(403).body(Map.of("message", "Forbidden."));
                    }
                    orderId = rs.getInt("id");
                    status = rs.getString("status");
                    orderDate = rs.getTimestamp("order_date").toLocalDateTime();
                    userId = rs.getInt("user_id");
                }
            }
            if (!"DELIVERED".equals(status)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Only delivered orders can be returned."));
            }
            if (ChronoUnit.DAYS.between(orderDate, LocalDateTime.now()) > 14) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Return period expired (14 days)."));
            }
            try (PreparedStatement s = conn.prepareStatement(
                    "SELECT id, status FROM return_requests WHERE order_id = ? LIMIT 1")) {
                s.setInt(1, orderId);
                try (ResultSet rs = s.executeQuery()) {
                    if (rs.next()) {
                        return ResponseEntity.badRequest().body(Map.of(
                                "message", "Return already requested.",
                                "status", rs.getString("status")));
                    }
                }
            }
            int requestId;
            try (PreparedStatement s = conn.prepareStatement(
                    "INSERT INTO return_requests (order_id, user_id, status) VALUES (?, ?, 'RETURN_REQUESTED')",
                    Statement.RETURN_GENERATED_KEYS)) {
                s.setInt(1, orderId);
                s.setInt(2, userId);
                s.executeUpdate();
                try (ResultSet keys = s.getGeneratedKeys()) {
                    keys.next();
                    requestId = keys.getInt(1);
                }
            }
            notifications.notify(userId, "تم استلام طلب الإرجاع",
                    "طلب الإرجاع للطلب " + code + " قيد المراجعة الآن.", "RETURN");
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("success", true);
            out.put("id", requestId);
            out.put("status", "RETURN_REQUESTED");
            return ResponseEntity.ok(out);
        } catch (SQLException e) {
            return ResponseEntity.status(500).body(Map.of("message", "Request failed."));
        }
    }

    @GetMapping
    public ResponseEntity<?> mine(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String userId) {
        String email = AuthTokenStore.resolve(authorization);
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Login required."));
        }
        if (!email.equalsIgnoreCase(userId.trim())) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden."));
        }
        List<Map<String, Object>> out = new ArrayList<>();
        String sql = "SELECT r.id, r.status, r.created_at, r.updated_at, o.order_code"
                + " FROM return_requests r JOIN orders o ON o.id = r.order_id"
                + " JOIN users u ON u.id = r.user_id WHERE u.email = ? ORDER BY r.id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(sql)) {
            s.setString(1, email.toLowerCase());
            try (ResultSet rs = s.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getInt("id"));
                    m.put("orderCode", rs.getString("order_code"));
                    m.put("status", rs.getString("status"));
                    m.put("created_at", rs.getTimestamp("created_at").toLocalDateTime().toString());
                    m.put("updated_at", rs.getTimestamp("updated_at").toLocalDateTime().toString());
                    out.add(m);
                }
            }
        } catch (SQLException e) {
            return ResponseEntity.status(500).body(Map.of("message", "Unavailable."));
        }
        return ResponseEntity.ok(out);
    }

    private static int toIntOr(String v, int fallback) {
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
