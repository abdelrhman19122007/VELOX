package com.app.controller;

import com.app.dao.WebOrderDAO;
import com.app.service.AuthTokenStore;
import com.app.service.OrderService;
import com.app.util.DatabaseConnection;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Complaints & suggestions. POST /api/complaints {orderId?, details}
 * Stored in MySQL complaints (replaces the console-only fake form).
 */
@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final WebOrderDAO lookup = new WebOrderDAO();
    private final OrderService orders = new OrderService();

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
        Object details = body.get("details");
        if (details == null || String.valueOf(details).isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Details are required."));
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
        String sql = "INSERT INTO complaints (user_id, order_id, details) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setInt(1, userId);
            if (orderDbId == null) {
                s.setNull(2, java.sql.Types.BIGINT);
            } else {
                s.setInt(2, orderDbId);
            }
            s.setString(3, String.valueOf(details));
            s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) {
                keys.next();
                int complaintId = keys.getInt(1);
                new com.app.service.NotificationService().notify(userId,
                        "تم استلام شكوتك #" + complaintId,
                        "شكوتك قيد المراجعة وسنبلغك بأي تحديث.",
                        "COMPLAINT");
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("success", true);
                out.put("id", complaintId);
                out.put("status", "PENDING");
                return ResponseEntity.ok(out);
            }
        } catch (SQLException e) {
            return ResponseEntity.status(500).body(Map.of("message", "Submit failed."));
        }
    }

    /** Owner's complaint list with live statuses (for the support tracker). */
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
        Integer uid = lookup.findUserId(email);
        if (uid == null) {
            return ResponseEntity.ok(new java.util.ArrayList<>());
        }
        java.util.List<Map<String, Object>> out = new java.util.ArrayList<>();
        String sql = "SELECT c.id, c.details, c.status, c.created_at, o.order_code"
                + " FROM complaints c LEFT JOIN orders o ON o.id = c.order_id"
                + " WHERE c.user_id = ? ORDER BY c.id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(sql)) {
            s.setInt(1, uid);
            try (ResultSet rs = s.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getInt("id"));
                    m.put("details", rs.getString("details"));
                    m.put("status", rs.getString("status"));
                    m.put("created_at", rs.getTimestamp("created_at").toLocalDateTime().toString());
                    m.put("orderCode", rs.getString("order_code"));
                    out.add(m);
                }
            }
        } catch (SQLException e) {
            return ResponseEntity.status(500).body(Map.of("message", "Unavailable."));
        }
        return ResponseEntity.ok(out);
    }

    /**
     * Owner-only forward transition (PENDING -> IN_REVIEW -> RESOLVED)
     * with an automatic notification on every change.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<?> setStatus(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable("id") int id,
            @RequestBody Map<String, Object> body) {
        String email = AuthTokenStore.resolve(authorization);
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Login required."));
        }
        Object st = body.get("status");
        if (st == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "status is required."));
        }
        String next;
        try {
            next = com.app.enums.ComplaintStatus.valueOf(String.valueOf(st).trim().toUpperCase()).name();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Unknown status."));
        }
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(
                     "SELECT c.status, c.user_id FROM complaints c JOIN users u ON u.id = c.user_id"
                             + " WHERE c.id = ? LIMIT 1")) {
            s.setInt(1, id);
            try (ResultSet rs = s.executeQuery()) {
                if (!rs.next()) {
                    return ResponseEntity.notFound().build();
                }
                Integer owner = lookup.findUserId(email);
                if (owner == null || rs.getInt("user_id") != owner) {
                    return ResponseEntity.status(403).body(Map.of("message", "Forbidden."));
                }
                String current = rs.getString("status");
                List<String> flow = List.of("PENDING", "IN_REVIEW", "RESOLVED");
                if (!flow.contains(next) || flow.indexOf(next) != flow.indexOf(current) + 1) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "message", "Invalid transition: " + current + " -> " + next + "."));
                }
            }
            try (PreparedStatement u = conn.prepareStatement(
                    "UPDATE complaints SET status = ? WHERE id = ?")) {
                u.setString(1, next);
                u.setInt(2, id);
                u.executeUpdate();
            }
        } catch (SQLException e) {
            return ResponseEntity.status(500).body(Map.of("message", "Update failed."));
        }
        Integer uid = lookup.findUserId(email);
        if (uid != null) {
            new com.app.service.NotificationService().notify(uid,
                    "تحديث الشكوى #" + id,
                    "حالة شكوتك أصبحت الآن: " + next + ".", "COMPLAINT");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("id", id);
        out.put("status", next);
        return ResponseEntity.ok(out);
    }

    private Integer resolveDbId(String codeOrId) {
        try {
            return Integer.parseInt(codeOrId.trim());
        } catch (NumberFormatException e) {
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
}
