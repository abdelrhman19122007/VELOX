package com.app.controller;

import com.app.service.CustomerAccountService;
import com.app.service.OfferService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Personalized offers API (mirrors the console "offers for you" box).
 *
 * GET /api/offers/personalized?userId={phone}
 */
@RestController
@RequestMapping("/api/offers")
public class OfferController {

    @GetMapping("/personalized")
    public ResponseEntity<Map<String, Object>> personalized(@RequestParam String userId) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().build();
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
