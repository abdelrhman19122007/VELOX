package com.velox.catalog;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.velox.IntegrationTest;

/** Helpers for catalogue tests: an admin creates stores and products through the real API. */
abstract class CatalogApiTestBase extends IntegrationTest {

    protected String admin() {
        return login(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    /** Creates a FOOD store with a staff account; returns {storeId, staffEmail}. */
    protected String[] newStore(String admin, String nameAr, String opens, String closes) {
        String email = "store" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        Map<String, Object> body = new HashMap<>();
        body.put("type", "FOOD");
        body.put("cuisine", "GRILLS");
        body.put("name", "Test Grill");
        body.put("nameAr", nameAr);
        body.put("phone", "01012345678");
        body.put("opens", opens);
        body.put("closes", closes);
        body.put("accountEmail", email);
        body.put("accountPassword", "store-pass-123");
        String id = post("/api/admin/stores", body, admin).getBody().get("id").asText();
        return new String[] {id, email};
    }

    protected String newProduct(String admin, String storeId, String nameAr, String section) {
        return post("/api/admin/stores/" + storeId + "/products", Map.of("name", "Kofta", "nameAr", nameAr,
                "price", 150, "section", section), admin).getBody().get("id").asText();
    }
}
