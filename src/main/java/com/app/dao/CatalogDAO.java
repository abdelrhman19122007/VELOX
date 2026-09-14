package com.app.dao;

import com.app.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only catalog queries feeding the storefront API.
 * Shapes intentionally match what the frontend expects
 * (bilingual fallbacks, frontend category ids, emoji per type).
 */
public class CatalogDAO {

    public List<Map<String, Object>> listAvailableProducts() {
        String sql = "SELECT p.id, p.name, p.name_ar, p.description, p.description_ar, p.price,"
                + " p.stock_quantity, p.product_type, p.size, p.store_id, p.image_url,"
                + " s.name AS store_name, s.name_ar AS store_ar"
                + " FROM products p JOIN stores s ON s.id = p.store_id"
                + " WHERE p.is_available = 1 ORDER BY p.store_id, p.id";
        List<Map<String, Object>> out = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                out.add(row(rs));
            }
        } catch (SQLException e) {
            System.err.println("[CatalogDAO] list failed: " + e.getMessage());
        }
        return out;
    }

    public Map<String, Integer> countByFrontendCategory() {
        String sql = "SELECT product_type, COUNT(*) AS c FROM products"
                + " WHERE is_available = 1 GROUP BY product_type";
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("food", 0);
        counts.put("fashion", 0);
        counts.put("electronics", 0);
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                counts.put(toFrontendCategory(rs.getString("product_type")), rs.getInt("c"));
            }
        } catch (SQLException e) {
            System.err.println("[CatalogDAO] count failed: " + e.getMessage());
        }
        return counts;
    }

    public static String toFrontendCategory(String productType) {
        if (productType == null) {
            return "food";
        }
        return switch (productType.trim().toUpperCase()) {
            case "CLOTHES" -> "fashion";
            case "TECH" -> "electronics";
            default -> "food";
        };
    }

    public static String emojiFor(String productType) {
        if (productType == null) {
            return "📦";
        }
        return switch (productType.trim().toUpperCase()) {
            case "CLOTHES" -> "👕";
            case "TECH" -> "📱";
            default -> "🍔";
        };
    }

    private static Map<String, Object> row(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        String nameAr = orFallback(rs.getString("name_ar"), name);
        String desc = orFallback(rs.getString("description"), "");
        String descAr = orFallback(rs.getString("description_ar"), desc);
        String type = rs.getString("product_type");
        String store = rs.getString("store_name");
        String storeAr = orFallback(rs.getString("store_ar"), store);
        String image = rs.getString("image_url");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("nameEn", name);
        m.put("nameAr", nameAr);
        m.put("storeEn", store);
        m.put("storeAr", storeAr);
        m.put("category", toFrontendCategory(type));
        m.put("price", rs.getDouble("price"));
        m.put("stock", rs.getInt("stock_quantity"));
        m.put("emoji", emojiFor(type));
        if (image != null && !image.isBlank()) {
            m.put("image", image);
        } else {
            m.put("image", "assets/images/velox-logo.jpeg");
        }
        m.put("descEn", desc);
        m.put("descAr", descAr);
        m.put("store_id", rs.getInt("store_id"));
        return m;
    }

    private static String orFallback(String v, String fallback) {
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
