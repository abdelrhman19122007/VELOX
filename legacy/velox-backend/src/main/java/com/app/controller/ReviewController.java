package com.app.controller;

import com.app.dao.WebOrderDAO;
import com.app.service.AuthTokenStore;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * App ratings. POST /api/reviews {orderId?, rating 1-5, comment?}
 * Stored in MySQL reviews (replaces the console-only fake form).
 * GET /api/reviews?storeId= public comments for a store page.
 */
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final WebOrderDAO lookup = new WebOrderDAO();
    private final OrderService orders = new OrderService();

    /** Public review comments for a store details page (latest first). */
    @GetMapping
    public ResponseEntity<?> forStore(@RequestParam int storeId) {
        String sql = "SELECT u.full_name AS author, r.rating, r.comment, r.created_at"
                + " FROM reviews r JOIN users u ON u.id = r.user_id"
                + " JOIN orders o ON o.id = r.order_id"
                + " JOIN order_items oi ON oi.order_id = o.id"
                + " JOIN products p ON p.id = oi.product_id"
                + " WHERE p.store_id = ? AND r.comment IS NOT NULL AND r.comment <> ''"
                + " GROUP BY r.id ORDER BY r.created_at DESC LIMIT 20";
        List<Map<String, Object>> out = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(sql)) {
            s.setInt(1, storeId);
            try (ResultSet rs = s.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("author", firstName(rs.getString("author")));
                    row.put("rating", rs.getInt("rating"));
                    row.put("comment", rs.getString("comment"));
                    row.put("createdAt", String.valueOf(rs.getTimestamp("created_at")));
                    out.add(row);
                }
            }
        } catch (SQLException e) {
            return ResponseEntity.status(500).body(Map.of("message", "Reviews unavailable."));
        }
        return ResponseEntity.ok(out);
    }

    private static String firstName(String full) {
        if (full == null || full.isBlank()) {
            return "VELOX user";
        }
        return full.trim().split("\\s+")[0];
    }

    @PostMapping
    public ResponseEntity<?> submit(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        String email = AuthTokenStore.resolve(authorization);
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Login required."));
        }
        Integer userId = lookup.findUserId(email);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Login required."));
        }
        int rating = toInt(body.get("rating"), -1);
        if (rating < 1 || rating > 5) {
            return ResponseEntity.badRequest().body(Map.of("message", "Rating must be 1-5."));
        }
        Integer orderDbId = null;
        Object oid = body.get("orderId") != null ? body.get("orderId") : body.get("order_id");
        if (oid != null && !String.valueOf(oid).isBlank()) {
            String code = String.valueOf(oid).trim();
            try {
                orders.getTracking(code);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("message", "Order not found."));
            }
            if (!orders.ownsOrder(code, email)) {
                return ResponseEntity.status(403).body(Map.of("message", "Forbidden."));
            }
            orderDbId = resolveDbId(code);
        }
        Object comment = body.get("comment");
        String sql = "INSERT INTO reviews (user_id, order_id, rating, comment) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setInt(1, userId);
            if (orderDbId == null) {
                s.setNull(2, java.sql.Types.BIGINT);
            } else {
                s.setInt(2, orderDbId);
            }
            s.setInt(3, rating);
            s.setString(4, comment == null ? "" : String.valueOf(comment));
            s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) {
                keys.next();
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("success", true);
                out.put("id", keys.getInt(1));
                return ResponseEntity.ok(out);
            }
        } catch (SQLException e) {
            return ResponseEntity.status(500).body(Map.of("message", "Submit failed."));
        }
    }

    private Integer resolveDbId(String codeOrId) {
        try {
            return Integer.parseInt(codeOrId.trim());
        } catch (NumberFormatException e) {
            // order_code -> numeric id
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement s = conn.prepareStatement(
                         "SELECT id FROM orders WHERE order_code = ? LIMIT 1")) {
                s.setString(1, codeOrId.trim());
                try (ResultSet rs = s.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            } catch (SQLException ignored) {
            }
            return null;
        }
    }

    private static int toInt(Object v, int fallback) {
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (Exception e) {
            return fallback;
        }
    }
}
