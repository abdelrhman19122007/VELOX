package com.app.controller;

import com.app.service.PromoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Public coupon codes powering the offers section.
 *
 * GET  /api/coupons
 * POST /api/coupons/validate {code, subtotal}
 */
@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final PromoService promos = new PromoService();

    @GetMapping
    public ResponseEntity<?> list() {
        try {
            return ResponseEntity.ok(promos.activeCoupons());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(500).body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validate(@RequestBody Map<String, Object> body) {
        Object code = body.get("code");
        if (code == null || String.valueOf(code).isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "code is required."));
        }
        double subtotal = toDouble(body.get("subtotal"), 0);
        PromoService.Coupon coupon = promos.find(String.valueOf(code));
        String problem = PromoService.rejectionReason(coupon, subtotal, LocalDateTime.now()).orElse(null);
        if (problem != null) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "message", problem));
        }
        PromoService.Applied applied = PromoService.apply(coupon, subtotal);
        return ResponseEntity.ok(Map.of(
                "valid", true,
                "code", coupon.code(),
                "discount", applied.discount(),
                "freeShipping", applied.freeShipping()));
    }

    private static double toDouble(Object v, double fallback) {
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(v).trim());
        } catch (Exception e) {
            return fallback;
        }
    }
}
