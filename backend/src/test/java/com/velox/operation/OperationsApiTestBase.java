package com.velox.operation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.velox.IntegrationTest;

/** Helpers: stores, customers, drivers, and an order taken all the way to the customer's door. */
abstract class OperationsApiTestBase extends IntegrationTest {

    protected static final String STORE_PASSWORD = "store-pass-123";

    /** A store with one product; returns {storeId, productId, staffToken}. */
    protected String[] store(double price, String size) {
        String admin = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        String email = "staff" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String storeId = post("/api/admin/stores", Map.of("type", "GROCERY", "cuisine", "MARKET", "name", "Test Market", "nameAr", "سوبر ماركت التجربة",
                "accountEmail", email, "accountPassword", STORE_PASSWORD), admin).getBody().get("id").asText();
        String productId = post("/api/admin/stores/" + storeId + "/products", Map.of("name", "Item", "nameAr", "صنف",
                "price", price, "size", size, "section", "OTHER"), admin).getBody().get("id").asText();
        return new String[] {storeId, productId, login(email, STORE_PASSWORD)};
    }

    protected String[] store() {
        return store(100, "SMALL");
    }

    protected Map<String, Object> order(String productId, String method) {
        Map<String, Object> o = new HashMap<>();
        o.put("zone", "SAMANNOUD");
        o.put("phone", "01012345678");
        o.put("address", "شارع البحر، سمنود");
        o.put("items", List.of(Map.of("productId", productId, "qty", 1)));
        o.put("paymentMethod", method);
        return o;
    }

    /** A cash order; returns its code. */
    protected String placeCash(String customer, String productId) {
        return post("/api/orders", order(productId, "CASH"), customer).getBody().get("code").asText();
    }

    protected String customer() {
        return newCustomer(uniqueEmail(), "secret-123");
    }

    /** A driver who registered themselves and went online. */
    protected String onlineDriver(String vehicle) {
        String token = post("/api/auth/register", Map.of("name", "Driver", "email", uniqueEmail(), "phone", "01112345678",
                "password", "driver-pass-1", "role", "DRIVER"), null).getBody().get("token").asText();
        post("/api/driver/status", Map.of("online", true, "vehicle", vehicle), token);
        return token;
    }

    protected JsonNode act(String path, String token) {
        return post(path, null, token).getBody();
    }

    /** Store accepts and readies, a driver takes, picks up and delivers. */
    protected void deliver(String staff, String code) {
        String driver = onlineDriver("MOTORBIKE");
        act("/api/restaurant/orders/" + code + "/accept", staff);
        act("/api/restaurant/orders/" + code + "/ready", staff);
        act("/api/driver/orders/" + code + "/take", driver);
        act("/api/driver/orders/" + code + "/pickup", driver);
        act("/api/driver/orders/" + code + "/deliver", driver);
    }

    protected static boolean contains(JsonNode orders, String code) {
        for (JsonNode o : orders) {
            if (code.equals(o.get("code").asText())) {
                return true;
            }
        }
        return false;
    }
}
