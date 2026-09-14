package com.app.controller;

import com.app.dao.CatalogDAO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Storefront categories: GET /api/categories
 * Ids match the frontend filters: all/food/fashion/electronics.
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CatalogDAO catalog = new CatalogDAO();

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> all() {
        Map<String, Integer> counts = catalog.countByFrontendCategory();
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        List<Map<String, Object>> out = new ArrayList<>();
        out.add(cat("all", "✦", "كل المنتجات", "All products", total, "#e9ecff"));
        out.add(cat("food", "🍽️", "وجبات ومشروبات", "Meals & drinks", counts.get("food"), "#fff0d9"));
        out.add(cat("fashion", "👕", "أزياء وإكسسوارات", "Fashion picks", counts.get("fashion"), "#ffe8f2"));
        out.add(cat("electronics", "📱", "أجهزة وتقنيات", "Tech products", counts.get("electronics"), "#e9e4ff"));
        return ResponseEntity.ok(out);
    }

    private static Map<String, Object> cat(String id, String icon, String ar, String en, int n, String tint) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("icon", icon);
        m.put("descAr", ar + " — " + n);
        m.put("descEn", en + " — " + n);
        m.put("tint", tint);
        return m;
    }
}
