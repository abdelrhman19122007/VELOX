package com.velox.payment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.velox.IntegrationTest;
import com.velox.auth.repository.UserRepository;
import com.velox.security.AuthUser;

/** Helpers: a store with one product, order requests with a payment method, and the payment endpoints. */
abstract class PaymentApiTestBase extends IntegrationTest {

    protected static final String VF_PHONE = "01012345678";

    @Autowired
    protected UserRepository users;

    /** A store with one product; returns {storeId, productId, staffEmail}. */
    protected String[] storeWithProduct(double price) {
        String admin = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        String email = "staff" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String storeId = post("/api/admin/stores", Map.of("type", "FOOD", "cuisine", "KOSHARY", "name", "Test Koshary",
                "nameAr", "كشري التجربة", "accountEmail", email, "accountPassword", "store-pass-123"), admin)
                .getBody().get("id").asText();
        String productId = post("/api/admin/stores/" + storeId + "/products", Map.of("name", "Koshary", "nameAr", "كشري",
                "price", price, "section", "MEALS"), admin).getBody().get("id").asText();
        return new String[] {storeId, productId, email};
    }

    protected Map<String, Object> order(String productId, String method) {
        Map<String, Object> o = new HashMap<>();
        o.put("zone", "SAMANNOUD");
        o.put("phone", "01012345678");
        o.put("address", "شارع الجيش، سمنود");
        o.put("items", List.of(Map.of("productId", productId, "qty", 1)));
        o.put("paymentMethod", method);
        if ("VODAFONE_CASH".equals(method)) {
            o.put("walletPhone", VF_PHONE);
        }
        return o;
    }

    /** Places an order and returns it (code, status, paymentStatus, total…). */
    protected JsonNode place(String customer, Map<String, Object> order) {
        return post("/api/orders", order, customer).getBody();
    }

    protected JsonNode payment(String customer, String code) {
        return get("/api/orders/" + code + "/payment", customer).getBody();
    }

    protected JsonNode orderOf(String customer, String code) {
        return get("/api/orders/" + code, customer).getBody();
    }

    protected long idOf(String code) {
        return db.queryForObject("SELECT id FROM orders WHERE code = ?", Long.class, code);
    }

    protected AuthUser account(String email) {
        return users.findByEmail(email).orElseThrow().toAuthUser();
    }
}
