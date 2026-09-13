package com.app.controller;

import com.app.service.CustomerAccountService;
import com.app.service.LoyaltyService;
import com.app.service.OfferService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Auth API sharing the exact same accounts as the console app
 * ({@link CustomerAccountService}): email + password + governorate.
 *
 * POST /api/auth/register {email,password,name,phone,governorate}
 * POST /api/auth/login    {email,password}
 * GET  /api/auth/profile?userId={email}
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        try {
            CustomerAccountService.Profile p = CustomerAccountService.register(
                    body.get("email"), body.get("password"), body.get("name"),
                    body.get("phone"), body.get("governorate"));
            return ResponseEntity.ok(safeView(p));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            CustomerAccountService.Profile p = CustomerAccountService.login(
                    body.get("email"), body.get("password"));
            return ResponseEntity.ok(safeView(p));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(401).body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> profile(@RequestParam String userId) {
        String key = CustomerAccountService.keyFor(userId);
        CustomerAccountService.Profile p = CustomerAccountService.profileOf(key);
        if (p == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(safeView(p));
    }

    private Map<String, Object> safeView(CustomerAccountService.Profile p) {
        String key = CustomerAccountService.keyForEmail(p.email);
        List<String> offers = OfferService.personalizedOffers(
                CustomerAccountService.loadUserOrders(key));
        return Map.of(
                "email", p.email,
                "name", p.name,
                "phone", p.phone,
                "governorate", p.governorate,
                "successfulOrders", LoyaltyService.successfulCount(key),
                "loyalty", LoyaltyService.progressMessage(key),
                "topCategory", OfferService.topCategoryName(CustomerAccountService.loadUserOrders(key)),
                "offers", offers);
    }
}
