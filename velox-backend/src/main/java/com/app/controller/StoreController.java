package com.app.controller;

import com.app.util.DatabaseConnection;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Public store directory: every store with live product count and
 * average rating (from order reviews). Powers the "shop by store" flow.
 *
 * GET /api/stores
 */
@RestController
@RequestMapping("/api/stores")
public class StoreController {

    @GetMapping
    public ResponseEntity<?> list() {
        String sql = "SELECT s.id, s.name, s.name_ar, s.store_type,"
                + " COUNT(DISTINCT CASE WHEN p.is_available = 1 THEN p.id END) AS products,"
                + " COUNT(r.id) AS reviews, ROUND(AVG(r.rating), 1) AS rating"
                + " FROM stores s"
                + " LEFT JOIN products p ON p.store_id = s.id"
                + " LEFT JOIN order_items oi ON oi.product_id = p.id"
                + " LEFT JOIN orders o ON o.id = oi.order_id"
                + " LEFT JOIN reviews r ON r.order_id = o.id"
                + " GROUP BY s.id ORDER BY rating DESC, products DESC";
        List<Map<String, Object>> out = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(sql);
             ResultSet rs = s.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getInt("id"));
                row.put("name", rs.getString("name"));
                row.put("nameAr", rs.getString("name_ar"));
                row.put("type", rs.getString("store_type"));
                row.put("productsCount", rs.getInt("products"));
                row.put("reviewsCount", rs.getInt("reviews"));
                Object rating = rs.getObject("rating");
                row.put("rating", rating != null ? ((Number) rating).doubleValue() : null);
                out.add(row);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Stores unavailable."));
        }
        return ResponseEntity.ok(out);
    }
}
