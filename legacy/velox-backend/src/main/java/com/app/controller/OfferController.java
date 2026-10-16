package com.app.controller;

import com.app.service.AuthTokenStore;
import com.app.service.CustomerAccountService;
import com.app.service.OfferService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Personalized offers API (mirrors the console "offers for you" box).
 * Self-only: callers can read only their own offers.
 *
 * GET /api/offers/personalized?userId={email}
 */
@RestController
@RequestMapping("/api/offers")
public class OfferController {

    @GetMapping("/personalized")
    public ResponseEntity<?> personalized(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String userId) {
        String email = AuthTokenStore.resolve(authorization);
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Login required."));
        }
        if (!email.equalsIgnoreCase(userId.trim())) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden."));
        }
        String key = CustomerAccountService.keyFor(userId);
        List<String> offers = OfferService.personalizedOffers(
                CustomerAccountService.loadUserOrders(key));
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "topCategory", OfferService.topCategoryName(CustomerAccountService.loadUserOrders(key)),
                "offers", offers));
    }
}
