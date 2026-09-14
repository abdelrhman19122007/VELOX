package com.app.controller;

import com.app.service.AuthTokenStore;
import com.app.service.LoyaltyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loyalty progress, read from MySQL (self only).
 *
 * GET /api/loyalty/status?userId={email}
 */
@RestController
@RequestMapping("/api/loyalty")
public class LoyaltyController {

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
}
