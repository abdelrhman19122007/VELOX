package com.velox.order;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import com.velox.IntegrationTest;
import com.velox.auth.repository.UserRepository;
import com.velox.security.AuthUser;

/** Helpers: stores and products made through the admin API, customers, and order requests. */
abstract class OrderApiTestBase extends IntegrationTest {

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

    protected Map<String, Object> order(String productId, int qty) {
        Map<String, Object> o = new HashMap<>();
        o.put("zone", "SAMANNOUD");
        o.put("phone", "01012345678");
        o.put("address", "شارع الجيش، سمنود");
        o.put("landmark", "جنب الجامع الكبير");
        o.put("items", List.of(Map.of("productId", productId, "qty", qty, "note", "من غير بصل")));
        return o;
    }

    protected AuthUser account(String email) {
        return users.findByEmail(email).orElseThrow().toAuthUser();
    }
}
