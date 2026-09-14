package com.app.controller;

import com.app.dao.UserDAO;
import com.app.dao.WebOrderDAO;
import com.app.service.AuthTokenStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Wallet endpoints backed by users.current_budget/remaining_budget.
 *
 * GET  /api/wallet/balance?userId={email}   (self only)
 * POST /api/wallet/topup {amount}           (own account)
 */
@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final UserDAO users = new UserDAO();
    private final WebOrderDAO lookup = new WebOrderDAO();

    @GetMapping("/balance")
    public ResponseEntity<?> balance(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String userId) {
        Integer id = selfIdOrNull(authorization, userId);
        if (id == null) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden."));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("userId", userId);
        out.put("balance", users.getBalance(id));
        return ResponseEntity.ok(out);
    }

    @PostMapping("/topup")
    public ResponseEntity<?> topup(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        String email = AuthTokenStore.resolve(authorization);
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Login required."));
        }
        double amount = toDouble(body.get("amount"), -1);
        if (amount <= 0 || amount > 1_000_000) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid amount."));
        }
        Integer id = lookup.findUserId(email);
        if (id == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Login required."));
        }
        // Optional card top-up: validate (Luhn) without ever storing the full number.
        boolean cardSaved = false;
        String cardNumber = body.get("card_number") != null ? String.valueOf(body.get("card_number")) : null;
        if (cardNumber == null && body.get("cardNumber") != null) {
            cardNumber = String.valueOf(body.get("cardNumber"));
        }
        if (cardNumber != null && !cardNumber.isBlank()) {
            String digits = cardNumber.replaceAll("[\\s-]", "");
            if (!digits.matches("\\d{13,19}") || !luhnOk(digits)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid card number."));
            }
            Object saveFlag = body.get("save_card") != null ? body.get("save_card") : body.get("saveCard");
            boolean save = saveFlag != null && String.valueOf(saveFlag).equalsIgnoreCase("true");
            if (save) {
                Object holder = body.get("cardholder_name") != null ? body.get("cardholder_name") : body.get("cardholderName");
                String name = holder == null ? "" : String.valueOf(holder);
                cardSaved = users.saveCard(id, name, digits.substring(digits.length() - 4), brandOf(digits));
            }
        }
        if (!users.topUp(id, amount)) {
            return ResponseEntity.status(500).body(Map.of("message", "Top-up failed."));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("balance", users.getBalance(id));
        out.put("cardSaved", cardSaved);
        return ResponseEntity.ok(out);
    }

    private static boolean luhnOk(String digits) {
        int sum = 0;
        boolean alternate = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n -= 9;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    private static String brandOf(String digits) {
        if (digits.startsWith("4")) {
            return "Visa";
        }
        if (digits.matches("5[1-5].*") || digits.matches("2(2[1-9]|[3-6][0-9]|7[01][0-9]|720[0-9]).*")) {
            return "Mastercard";
        }
        if (digits.matches("3[47].*")) {
            return "Amex";
        }
        if (digits.startsWith("6")) {
            return "Discover";
        }
        return "Unknown";
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
