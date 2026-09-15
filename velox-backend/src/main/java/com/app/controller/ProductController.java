package com.app.controller;

import com.app.dao.CatalogDAO;
import com.app.util.DatabaseConnection;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Storefront catalog: GET /api/products, GET /api/products/popular
 * Shapes match the frontend catalog service (bilingual fallbacks included).
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final CatalogDAO catalog = new CatalogDAO();

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> all() {
        return ResponseEntity.ok(catalog.listAvailableProducts());
    }

    /** Most-ordered dishes/products by sold quantity (for the home section). */
    @GetMapping("/popular")
    public ResponseEntity<List<Map<String, Object>>> popular(
            @RequestParam(defaultValue = "8") int limit) {
        int n = Math.min(Math.max(limit, 1), 20);
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT oi.product_id, SUM(oi.quantity) AS sold FROM order_items oi"
                + " JOIN orders o ON o.id = oi.order_id"
                + " WHERE o.status <> 'RETURNED'"
                + " GROUP BY oi.product_id ORDER BY sold DESC LIMIT ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(sql)) {
            s.setInt(1, n);
            try (ResultSet rs = s.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("product_id"));
                }
            }
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
        Map<Integer, Map<String, Object>> byId = new LinkedHashMap<>();
        for (Map<String, Object> p : catalog.listAvailableProducts()) {
            Object id = p.get("id");
            if (id instanceof Number num) {
                byId.put(num.intValue(), p);
            }
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Integer id : ids) {
            if (byId.containsKey(id)) {
                out.add(byId.get(id));
            }
        }
        return ResponseEntity.ok(out);
    }
}
