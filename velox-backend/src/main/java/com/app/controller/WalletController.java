package com.app.controller;

import com.app.dao.UserDAO;
import com.app.dao.WebOrderDAO;
import com.app.service.AuthTokenStore;
import com.app.service.RefundService;
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
    private final RefundService refunds = new RefundService();

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
        // Recharge source: e-wallet (Vodafone Cash / WE Pay) or credit card.
        // Only references (provider / last-4) are stored, never secrets.
        boolean methodSaved = false;
        String methodSavedAs = null;
        String method = first(body, "method", "recharge_method");
        String cardNumber = first(body, "card_number", "cardNumber");
        String walletNumber = first(body, "wallet_number", "walletNumber", "phone");
        String providerRaw = first(body, "provider", "wallet_provider");
        if (method == null) {
            method = cardNumber != null ? "CARD" : (walletNumber != null ? "WALLET" : null);
        } else {
            method = method.trim().toUpperCase();
        }
        if (method == null || (!method.equals("CARD") && !method.equals("WALLET"))) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Choose recharge method: CARD or WALLET (Vodafone Cash / WE Pay)."));
        }
        Object saveFlag = body.get("save_card") != null ? body.get("save_card") : body.get("saveCard");
        boolean save = saveFlag != null && String.valueOf(saveFlag).equalsIgnoreCase("true");
        Object holderRaw = body.get("cardholder_name") != null ? body.get("cardholder_name") : body.get("cardholderName");
        String holder = holderRaw == null ? "" : String.valueOf(holderRaw);
        if (method.equals("WALLET")) {
            String provider = normalizeWalletProvider(providerRaw);
            if (provider == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Wallet provider must be Vodafone Cash or WE Pay."));
            }
            if (walletNumber == null || !walletNumber.trim().matches("01\\d{9}")) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid wallet number."));
            }
            String digits = walletNumber.trim().replaceAll("[^0-9]", "");
            if (save) {
                methodSaved = users.saveCard(id, holder, digits.substring(digits.length() - 4),
                        providerLabel(provider), "WALLET", provider);
                methodSavedAs = providerLabel(provider);
            }
        } else {
            if (cardNumber == null || cardNumber.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Card number is required."));
            }
            String digits = cardNumber.replaceAll("[\\s-]", "");
            if (!digits.matches("\\d{13,19}") || !luhnOk(digits)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid card number."));
            }
            if (save) {
                methodSaved = users.saveCard(id, holder, digits.substring(digits.length() - 4),
                        brandOf(digits), "CARD", brandOf(digits));
                methodSavedAs = brandOf(digits);
            }
        }
        if (!users.topUp(id, amount)) {
            return ResponseEntity.status(500).body(Map.of("message", "Top-up failed."));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("balance", users.getBalance(id));
        out.put("methodSaved", methodSaved);
        if (methodSavedAs != null) {
            out.put("savedAs", methodSavedAs);
        }
        out.put("cardSaved", methodSaved);
        return ResponseEntity.ok(out);
    }

    /**
     * Feature 2: instant wallet compensation.
     * POST /api/wallet/refund {orderId, reason: LATE|WRONG_ITEM|DAMAGED}
     * Rules live in RefundPolicy; one refund per order (DB-unique).
     */
    @PostMapping("/refund")
    public ResponseEntity<?> refund(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        String email = AuthTokenStore.resolve(authorization);
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Login required."));
        }
        Object id = body.get("orderId") != null ? body.get("orderId") : body.get("order_id");
        Object reason = body.get("reason");
        if (id == null || reason == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "orderId and reason are required."));
        }
        try {
            return ResponseEntity.ok(refunds.claim(email,
                    String.valueOf(id), String.valueOf(reason)));
        } catch (SecurityException ex) {
            return ResponseEntity.status(403).body(Map.of("message", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    private static String first(Map<String, Object> body, String... keys) {
        for (String k : keys) {
            if (body.get(k) != null && !String.valueOf(body.get(k)).isBlank()) {
                return String.valueOf(body.get(k));
            }
        }
        return null;
    }

    private static String normalizeWalletProvider(String raw) {
        // Generic e-wallets only (no brand-specific options); legacy
        // Vodafone/WE values still map for backward compatibility.
        if (raw == null || raw.isBlank()) {
            return "E_WALLET";
        }
        String c = raw.trim().toUpperCase().replace(" ", "_");
        if (c.contains("VODAFONE")) {
            return "VODAFONE_CASH";
        }
        if (c.equals("WE_PAY") || c.equals("WE") || c.contains("WE_PAY")) {
            return "WE_PAY";
        }
        return "E_WALLET";
    }

    private static String providerLabel(String code) {
        if ("VODAFONE_CASH".equals(code)) {
            return "Vodafone Cash";
        }
        if ("WE_PAY".equals(code)) {
            return "WE Pay";
        }
        return "E-Wallet";
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
