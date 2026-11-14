package com.velox.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;

/** Admin creates stores; store staff manage their own menu; customers browse. */
class StoreApiTest extends CatalogApiTestBase {

    @Test
    void adminCreatesStoreWithItsOwnSignIn() {
        String admin = admin();
        String[] store = newStore(admin, "مشويات التجربة", "", "");
        assertTrue(store[0].matches("R\\d+"), "food stores get R ids");
        String productId = newProduct(admin, store[0], "طبق كفتة", "MEALS");
        assertTrue(productId.matches("F\\d+"));

        JsonNode detail = get("/api/stores/" + store[0], null).getBody();
        assertEquals("مشويات التجربة", detail.get("store").get("nameAr").asText());
        assertEquals("MEALS", detail.get("sections").get(0).get("code").asText());
        assertEquals("طبق كفتة", detail.get("sections").get(0).get("products").get(0).get("nameAr").asText());
        assertTrue(detail.get("store").get("openNow").asBoolean(), "no hours = always open");

        String staff = login(store[1], "store-pass-123");
        assertEquals(store[0], get("/api/auth/me", staff).getBody().get("storeId").asText());
    }

    @Test
    void onlyAdminsCreateStores() {
        String customer = newCustomer(uniqueEmail(), "secret-123");
        ResponseEntity<JsonNode> r = post("/api/admin/stores", Map.of("type", "FOOD", "cuisine", "GRILLS", "name", "X",
                "nameAr", "س", "accountEmail", uniqueEmail(), "accountPassword", "store-pass-123"), customer);
        assertEquals(HttpStatus.FORBIDDEN, r.getStatusCode());
    }

    @Test
    void storedXssAndBadHoursAreRefused() {
        String admin = admin();
        Map<String, Object> xss = new HashMap<>(Map.of("type", "FOOD", "cuisine", "GRILLS",
                "name", "<script>alert(1)</script>", "nameAr", "س", "accountEmail", uniqueEmail(), "accountPassword", "store-pass-123"));
        assertEquals(HttpStatus.BAD_REQUEST, post("/api/admin/stores", xss, admin).getStatusCode());

        Map<String, Object> halfHours = new HashMap<>(Map.of("type", "FOOD", "cuisine", "GRILLS", "name", "Shop",
                "nameAr", "س", "opens", "10:00", "accountEmail", uniqueEmail(), "accountPassword", "store-pass-123"));
        ResponseEntity<JsonNode> r = post("/api/admin/stores", halfHours, admin);
        assertEquals("INVALID_HOURS", r.getBody().get("code").asText());
    }

    @Test
    void staffSwitchAndProductChangesShowToCustomers() {
        String admin = admin();
        String[] store = newStore(admin, "كبدة", "", "");
        String productId = newProduct(admin, store[0], "سندوتش كبدة", "SANDWICHES");
        String staff = login(store[1], "store-pass-123");

        call(HttpMethod.PATCH, "/api/restaurant/store", Map.of("accepting", false), staff);
        assertFalse(get("/api/stores/" + store[0], null).getBody().get("store").get("openNow").asBoolean());

        assertEquals(HttpStatus.NO_CONTENT, call(HttpMethod.PATCH, "/api/restaurant/products/" + productId,
                Map.of("available", false, "price", 25.5), staff).getStatusCode());
        JsonNode product = get("/api/stores/" + store[0], null).getBody().get("sections").get(0).get("products").get(0);
        assertFalse(product.get("available").asBoolean());
        assertEquals(25.5, product.get("price").asDouble());
    }

    @Test
    void storeCannotTouchAnotherStoresProduct() {
        String admin = admin();
        String[] victim = newStore(admin, "المحل الأول", "", "");
        String victimProduct = newProduct(admin, victim[0], "منتج", "MEALS");
        String[] attacker = newStore(admin, "المحل التاني", "", "");
        String attackerStaff = login(attacker[1], "store-pass-123");

        ResponseEntity<JsonNode> r = call(HttpMethod.PATCH, "/api/restaurant/products/" + victimProduct,
                Map.of("price", 1), attackerStaff);
        assertEquals(HttpStatus.NOT_FOUND, r.getStatusCode(), "IDOR: looks like it doesn't exist");
        assertEquals(150.0, get("/api/stores/" + victim[0], null).getBody()
                .get("sections").get(0).get("products").get(0).get("price").asDouble(), "price unchanged");
    }

    @Test
    void customersCannotUseStoreEndpoints() {
        String customer = newCustomer(uniqueEmail(), "secret-123");
        assertEquals(HttpStatus.FORBIDDEN, call(HttpMethod.PATCH, "/api/restaurant/store", Map.of("accepting", false), customer)
                .getStatusCode());
    }

    @Test
    void unknownStoreIs404() {
        assertEquals("UNKNOWN_STORE", get("/api/stores/R999999", null).getBody().get("code").asText());
    }
}
