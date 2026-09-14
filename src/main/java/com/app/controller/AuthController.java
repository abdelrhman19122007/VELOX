package com.app.controller;

import com.app.dao.WebOrderDAO;
import com.app.service.AuthTokenStore;
import com.app.service.CustomerAccountService;
import com.app.service.LoyaltyService;
import com.app.service.OfferService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Auth API sharing the exact same accounts as the console app
 * ({@link CustomerAccountService}): email + password + governorate.
 * Responses use the {token, user} shape the storefront expects.
 * Field aliases (name/full_name, phone/phone_number) are accepted
 * so old and new clients both work.
 *
 * POST /api/auth/register {email,password,name|full_name,phone|phone_number,governorate}
 * POST /api/auth/login    {email,password}
 * GET  /api/auth/profile?userId={email}
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final WebOrderDAO users = new WebOrderDAO();

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        try {
            CustomerAccountService.Profile p = CustomerAccountService.register(
                    body.get("email"), body.get("password"),
                    first(body.get("name"), body.get("full_name")),
                    first(body.get("phone"), body.get("phone_number")),
                    body.get("governorate"));
            return ResponseEntity.ok(sessionView(p));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            CustomerAccountService.Profile p = CustomerAccountService.login(
                    body.get("email"), body.get("password"));
            return ResponseEntity.ok(sessionView(p));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(401).body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> profile(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String userId) {
        String email = AuthTokenStore.resolve(authorization);
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Login required."));
        }
        if (!email.equalsIgnoreCase(userId.trim())) {
            Integer ownId = users.findUserId(email);
            if (ownId == null || !String.valueOf(ownId).equals(userId.trim())) {
                return ResponseEntity.status(403).body(Map.of("message", "Forbidden."));
            }
        }
        String lookup = userId;
        if (!userId.contains("@")) {
            try {
                String byId = users.findEmailById(Integer.parseInt(userId.trim()));
                if (byId != null) {
                    lookup = byId;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        CustomerAccountService.Profile p = CustomerAccountService.profileOf(lookup);
        if (p == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(userView(p));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization != null) {
            String h = authorization.trim();
            if (h.regionMatches(true, 0, "Bearer ", 0, 7)) {
                h = h.substring(7).trim();
            }
            AuthTokenStore.revoke(h);
        }
        return ResponseEntity.ok(Map.of("success", true));
    }

    /** Updates own profile fields (name/phone/governorate; email is immutable). */
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, String> body) {
        String email = AuthTokenStore.resolve(authorization);
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Login required."));
        }
        com.app.dao.UserDAO dao = new com.app.dao.UserDAO();
        Integer id = users.findUserId(email);
        if (id == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Login required."));
        }
        String name = body.get("name") != null ? body.get("name") : body.get("full_name");
        String phone = body.get("phone") != null ? body.get("phone") : body.get("phone_number");
        String govRaw = body.get("governorate");
        String gov = null;
        if (name != null) {
            name = name.trim();
            if (name.length() < 2) {
                return ResponseEntity.badRequest().body(Map.of("message", "Name is too short."));
            }
        }
        if (phone != null) {
            phone = phone.trim();
            if (!phone.matches("01\\d{9}")) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid phone number."));
            }
            String owner = dao.findEmailByPhone(phone);
            if (owner != null && !owner.equalsIgnoreCase(email)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Phone already in use."));
            }
        }
        if (govRaw != null) {
            try {
                gov = com.app.enums.Governorate.fromNameLenient(govRaw).name();
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("message", "Unknown governorate."));
            }
        }
        if (name == null && phone == null && gov == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nothing to update."));
        }
        if (!dao.updateProfile(id, name, phone, gov)) {
            return ResponseEntity.status(500).body(Map.of("message", "Update failed."));
        }
        CustomerAccountService.Profile p = CustomerAccountService.profileOf(email);
        return ResponseEntity.ok(userView(p));
    }

    private Map<String, Object> sessionView(CustomerAccountService.Profile p) {
        Map<String, Object> view = userView(p);
        view.put("token", AuthTokenStore.issue(p.email));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("token", view.remove("token"));
        out.put("user", view);
        return out;
    }

    private Map<String, Object> userView(CustomerAccountService.Profile p) {
        String key = CustomerAccountService.keyForEmail(p.email);
        List<String> offers = OfferService.personalizedOffers(
                CustomerAccountService.loadUserOrders(key));
        Map<String, Object> view = new LinkedHashMap<>();
        Integer dbId = users.findUserId(p.email);
        if (dbId != null) {
            view.put("id", dbId);
        }
        view.put("email", p.email);
        view.put("name", p.name);
        view.put("full_name", p.name);
        view.put("phone", p.phone);
        view.put("phone_number", p.phone);
        view.put("governorate", p.governorate);
        view.put("successfulOrders", LoyaltyService.successfulCount(key));
        view.put("loyalty", LoyaltyService.progressMessage(key));
        view.put("topCategory", OfferService.topCategoryName(CustomerAccountService.loadUserOrders(key)));
        view.put("offers", offers);
        return view;
    }

    private static String first(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }
}
