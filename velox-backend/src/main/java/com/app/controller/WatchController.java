package com.app.controller;

import com.app.service.AuthTokenStore;
import com.app.service.PriceWatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Feature 5: price-drop watches (owner only).
 *
 * GET    /api/watches?userId={email}
 * POST   /api/watches {product_id, target_price?}  (no target = any drop)
 * DELETE /api/watches/{productId}
 */
@RestController
@RequestMapping("/api/watches")
public class WatchController {

    private final PriceWatchService watches = new PriceWatchService();

    @GetMapping
    public ResponseEntity<?> list(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String userId) {
        String email = AuthTokenStore.resolve(authorization);
        if (email == null || !email.equalsIgnoreCase(userId.trim())) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden."));
        }
        return ResponseEntity.ok(watches.list(email));
    }

    @PostMapping
    public ResponseEntity<?> watch(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        String email = AuthTokenStore.resolve(authorization);
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Login required."));
        }
        Object pid = body.get("product_id") != null ? body.get("product_id") : body.get("productId");
        if (pid == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "product_id is required."));
        }
        int productId;
        try {
            productId = pid instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(pid).trim());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid product_id."));
        }
        Object target = body.get("target_price") != null ? body.get("target_price") : body.get("targetPrice");
        Double targetPrice = null;
        if (target != null) {
            try {
                targetPrice = target instanceof Number n ? n.doubleValue()
                        : Double.parseDouble(String.valueOf(target).trim());
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid target_price."));
            }
        }
        try {
            return ResponseEntity.ok(watches.watch(email, productId, targetPrice));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<?> unwatch(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable int productId) {
        String email = AuthTokenStore.resolve(authorization);
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Login required."));
        }
        try {
            watches.unwatch(email, productId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }
}
