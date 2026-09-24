package com.app.controller;

import com.app.dao.WebOrderDAO;
import com.app.dto.OrderHistoryDto;
import com.app.dto.OrderTrackingDto;
import com.app.dto.PagedResponse;
import com.app.enums.OrderStatus;
import com.app.service.AuthTokenStore;
import com.app.service.OrderService;
import com.app.service.WebOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * KAN-90: Purchase History + Real-Time Order Status Tracking.
 *
 * GET   /api/orders/history?userId={id}&page=0&size=10
 * GET   /api/orders/{orderId}/tracking
 * PATCH /api/orders/{orderId}/status?status=SHIPPED (auth required)
 * POST  /api/orders {user_id?, items:[{product_id,quantity}], total_amount?, governorate?} (auth required)
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final WebOrderService webOrders = new WebOrderService();

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** Storefront checkout. Caller identity comes from the Bearer token. */
    @PostMapping
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        String email = AuthTokenStore.resolve(authorization);
        if (email == null) {
            // Compat: old clients send the numeric DB user_id instead of a token.
            // Non-numeric ids without a token are rejected (no impersonation).
            Object uid = body.get("user_id") != null ? body.get("user_id") : body.get("userId");
            if (uid != null) {
                try {
                    int id = uid instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(uid).trim());
                    email = new WebOrderDAO().findEmailById(id);
                } catch (NumberFormatException notNumeric) {
                    email = null;
                }
            }
        }
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Login required."));
        }
        try {
            if (com.app.util.DatabaseConnection.isLocalFallback()) {
                return ResponseEntity.ok(createLocalOrder(email, body));
            }
            Object items = body.get("items");
            Object gov = body.get("governorate");
            Object pay = body.get("paymentMethod") != null ? body.get("paymentMethod") : body.get("payment_method");
            Object scheduled = body.get("scheduled_for") != null ? body.get("scheduled_for") : body.get("scheduledFor");
            Object promo = body.get("promo_code") != null ? body.get("promo_code")
                    : (body.get("promoCode") != null ? body.get("promoCode") : body.get("coupon"));
            Object address = body.get("address") != null
                    ? body.get("address") : body.get("shipping_address");
            Map<String, Object> placed = webOrders.placeOrder(email,
                    (List<Map<String, Object>>) items,
                    gov == null ? null : String.valueOf(gov),
                    pay == null ? null : String.valueOf(pay),
                    scheduled == null ? null : String.valueOf(scheduled),
                    promo == null ? null : String.valueOf(promo),
                    address == null ? null : String.valueOf(address));
            return ResponseEntity.ok(placed);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(500).body(Map.of("message", ex.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> createLocalOrder(String email, Map<String, Object> body) {
        Object rawItems = body.get("items");
        if (!(rawItems instanceof List<?> items) || items.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty.");
        }
        double total = 0;
        Object rawTotal = body.get("total_amount") != null ? body.get("total_amount") : body.get("totalAmount");
        if (rawTotal instanceof Number n) total = Math.max(0, n.doubleValue());
        else if (rawTotal != null) {
            try { total = Math.max(0, Double.parseDouble(String.valueOf(rawTotal))); }
            catch (NumberFormatException ignored) { }
        }
        String governorate = String.valueOf(body.getOrDefault("governorate", "CAIRO"));
        String payment = String.valueOf(body.getOrDefault("paymentMethod", "CASH_ON_DELIVERY"));
        String sql = "INSERT INTO local_orders (email,total_amount,governorate,payment_method) VALUES (?,?,?,?)";
        try (var conn = com.app.util.DatabaseConnection.getConnection();
             var insert = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            insert.setString(1, email);
            insert.setDouble(2, total);
            insert.setString(3, governorate);
            insert.setString(4, payment);
            insert.executeUpdate();
            try (var keys = insert.getGeneratedKeys()) {
                if (!keys.next()) throw new IllegalStateException("Could not create order.");
                long id = keys.getLong(1);
                String code = "ORD-L" + id;
                try (var update = conn.prepareStatement("UPDATE local_orders SET order_code=? WHERE id=?")) {
                    update.setString(1, code);
                    update.setLong(2, id);
                    update.executeUpdate();
                }
                return Map.of("success", true, "orderId", id, "orderCode", code,
                        "total", total, "status", "PENDING", "paymentMethod", payment);
            }
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Checkout failed: " + e.getMessage());
        }
    }

    /** Feature 1: public multi-store price preview (no login needed). */
    @PostMapping("/quote")
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> quote(@RequestBody Map<String, Object> body) {
        try {
            Object items = body.get("items");
            Object gov = body.get("governorate");
            return ResponseEntity.ok(webOrders.quote(
                    (List<Map<String, Object>>) items,
                    gov == null ? null : String.valueOf(gov)));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<PagedResponse<OrderHistoryDto>> getHistory(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (!isSelf(authorization, userId)) {
            return ResponseEntity.status(403).build();
        }
        try {
            return ResponseEntity.ok(orderService.getOrdersByUserId(userId, page, size));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{orderId}/tracking")
    public ResponseEntity<OrderTrackingDto> getTracking(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String orderId) {
        String email = AuthTokenStore.resolve(authorization);
        if (email == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            orderService.getTracking(orderId);
            if (!orderService.ownsOrder(orderId, email)) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.ok(orderService.getTracking(orderId));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    /** One-click reorder: lines of a past order (owner only). */
    @GetMapping("/{orderId}/items")
    public ResponseEntity<?> items(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String orderId) {
        String email = AuthTokenStore.resolve(authorization);
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Login required."));
        }
        try {
            orderService.getTracking(orderId);
            if (!orderService.ownsOrder(orderId, email)) {
                return ResponseEntity.status(403).body(Map.of("message", "Forbidden."));
            }
            return ResponseEntity.ok(new WebOrderDAO().orderLines(orderId));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{orderId}/status")    public ResponseEntity<OrderTrackingDto> updateStatus(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String orderId,
            @RequestParam OrderStatus status) {
        String email = AuthTokenStore.resolve(authorization);
        if (email == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            // Must exist first (404), then must belong to the caller (403).
            orderService.getTracking(orderId);
            if (!orderService.ownsOrder(orderId, email)) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.ok(orderService.updateStatus(orderId, status));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    /** Caller may only see their own data: matching email or numeric DB id. */
    private boolean isSelf(String authorization, String userId) {
        String email = AuthTokenStore.resolve(authorization);
        if (email == null || userId == null) {
            return false;
        }
        if (email.equalsIgnoreCase(userId.trim())) {
            return true;
        }
        Integer ownId = new WebOrderDAO().findUserId(email);
        return ownId != null && String.valueOf(ownId).equals(userId.trim());
    }
}
