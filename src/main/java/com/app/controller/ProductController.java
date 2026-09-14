package com.app.controller;

import com.app.dao.CatalogDAO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Storefront catalog: GET /api/products
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
}
