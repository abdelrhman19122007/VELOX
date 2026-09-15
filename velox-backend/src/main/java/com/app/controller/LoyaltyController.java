package com.app.controller;

import com.app.service.AuthTokenStore;
import com.app.service.LoyaltyService;
import com.app.service.PlusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loyalty progress, read from MySQL (self only).
 *
 * GET  /api/loyalty/status?userId={email}
 * GET  /api/loyalty/subscription?userId={email}   (Feature 4: VELOX Plus)
 * POST /api/loyalty/subscribe                     (Feature 4: 50 EGP wallet)
 */
@RestController
@RequestMapping("/api/loyalty")
public class LoyaltyController {

    private final PlusService plus = new PlusService();
    private final com.app.dao.WebOrderDAO lookup = new com.app.dao.WebOrderDAO();

    @GetMapping("/status")
    public ResponseEntity<?> status(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String userId) {
        String email = AuthTokenStore.resolve(authorization);
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Login required."));
        }
        if (!email.equalsIgnoreCase(userId.trim())) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden."));
        }
        int ok = LoyaltyService.successfulCount(email);
        int earned = LoyaltyService.rewardsEarned(ok);
        int consumed = LoyaltyService.rewardsConsumed(email);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("successfulOrders", ok);
        out.put("rewardsEarned", earned);
        out.put("rewardsConsumed", consumed);
        out.put("rewardAvailable", earned > consumed);
        out.put("progressInCycle", ok % LoyaltyService.ORDERS_PER_REWARD);
        out.put("ordersPerReward", LoyaltyService.ORDERS_PER_REWARD);
        out.put("message", LoyaltyService.progressMessage(email));
        return ResponseEntity.ok(out);
    }

    @GetMapping("/subscription")
    public ResponseEntity<?> subscription(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String userId) {
        Integer id = selfIdOrNull(authorization, userId);
        if (id == null) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden."));
        }
        return ResponseEntity.ok(plus.statusOf(id));
    }

    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String email = AuthTokenStore.resolve(authorization);
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Login required."));
        }
        Integer id = lookup.findUserId(email);
        if (id == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Login required."));
        }
        try {
            return ResponseEntity.ok(plus.subscribe(id));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    private Integer selfIdOrNull(String authorization, String userId) {
        String email = AuthTokenStore.resolve(authorization);
        if (email == null || userId == null) {
            return null;
        }
        if (email.equalsIgnoreCase(userId.trim())) {
            return lookup.findUserId(email);
        }
        Integer ownId = lookup.findUserId(email);
        if (ownId != null && String.valueOf(ownId).equals(userId.trim())) {
            return ownId;
        }
        return null;
    }
}
