package com.velox.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;

/** Placing, viewing and cancelling orders over HTTP — including the attacks the orders module must stop. */
class OrderApiTest extends OrderApiTestBase {

    @Test
    void placeACashOrderPricedByTheServer() {
        String[] store = storeWithProduct(45);
        String customer = newCustomer(uniqueEmail(), "secret-123");
        Map<String, Object> body = order(store[1], 2);
        body.put("total", 1);                     // a client "price" is simply ignored
        body.put("cashChangeFor", 200);

        ResponseEntity<JsonNode> r = post("/api/orders", body, customer);
        assertEquals(HttpStatus.CREATED, r.getStatusCode());
        JsonNode o = r.getBody();
        assertTrue(o.get("code").asText().startsWith("ORD-"));
        assertEquals("PLACED", o.get("status").asText());
        assertEquals(90.0, o.get("subtotal").asDouble());
        assertEquals(15.0, o.get("deliveryFee").asDouble(), "Samannoud zone fee");
        assertEquals(105.0, o.get("total").asDouble());
        assertEquals("من غير بصل", o.get("items").get(0).get("note").asText());
        assertEquals("جنب الجامع الكبير", o.get("landmark").asText());
        assertEquals("كشري", o.get("items").get(0).get("nameAr").asText());
        assertEquals("PLACED", o.get("timeline").get(0).get("status").asText());

        assertEquals(1, get("/api/orders/mine", customer).getBody().size());
    }

    @Test
    void quoteMatchesTheOrder() {
        String[] store = storeWithProduct(30);
        JsonNode q = post("/api/orders/quote", Map.of("zone", "SAMANNOUD",
                "items", List.of(Map.of("productId", store[1], "qty", 3))), null).getBody();
        assertEquals(105.0, q.get("total").asDouble());
        assertTrue(q.get("etaMinutes").asInt() > 0);
    }

    @Test
    void cartRulesAreEnforced() {
        String[] a = storeWithProduct(20);
        String[] b = storeWithProduct(20);
        String customer = newCustomer(uniqueEmail(), "secret-123");

        Map<String, Object> mixed = order(a[1], 1);
        mixed.put("items", List.of(Map.of("productId", a[1], "qty", 1), Map.of("productId", b[1], "qty", 1)));
        assertEquals("MIXED_STORES", post("/api/orders", mixed, customer).getBody().get("code").asText());

        Map<String, Object> mars = order(a[1], 1);
        mars.put("zone", "MARS");
        assertEquals("UNKNOWN_ZONE", post("/api/orders", mars, customer).getBody().get("code").asText());

        assertEquals(HttpStatus.BAD_REQUEST, post("/api/orders", order(a[1], 0), customer).getStatusCode(), "qty 0");
        assertEquals(HttpStatus.BAD_REQUEST, post("/api/orders", order(a[1], 500), customer).getStatusCode(), "qty 500");

        Map<String, Object> change = order(a[1], 1);
        change.put("cashChangeFor", 10);
        assertEquals("INVALID_CHANGE", post("/api/orders", change, customer).getBody().get("code").asText());
    }

    @Test
    void soldOutAndClosedStoresCantBeOrdered() {
        String[] store = storeWithProduct(20);
        String staff = login(store[2], "store-pass-123");
        String customer = newCustomer(uniqueEmail(), "secret-123");

        call(HttpMethod.PATCH, "/api/restaurant/products/" + store[1], Map.of("available", false), staff);
        assertEquals("UNAVAILABLE", post("/api/orders", order(store[1], 1), customer).getBody().get("code").asText());

        call(HttpMethod.PATCH, "/api/restaurant/products/" + store[1], Map.of("available", true), staff);
        call(HttpMethod.PATCH, "/api/restaurant/store", Map.of("accepting", false), staff);
        assertEquals("STORE_CLOSED", post("/api/orders", order(store[1], 1), customer).getBody().get("code").asText());
    }

    @Test
    void errandNeedsNoStoreButNeedsDetails() {
        String customer = newCustomer(uniqueEmail(), "secret-123");
        Map<String, Object> errand = order("unused", 1);
        errand.remove("items");
        errand.put("kind", "ERRAND");
        assertEquals("INVALID_ERRAND", post("/api/orders", errand, customer).getBody().get("code").asText());

        errand.put("errandDetails", "2 كيلو طماطم وعيش من الفرن");
        JsonNode o = post("/api/orders", errand, customer).getBody();
        assertEquals("ERRAND", o.get("kind").asText());
        assertEquals(15.0, o.get("total").asDouble(), "only the delivery; the driver collects the shop receipt");
    }

    @Test
    void ordersArePrivate() {
        String[] store = storeWithProduct(20);
        String owner = newCustomer(uniqueEmail(), "secret-123");
        String code = post("/api/orders", order(store[1], 1), owner).getBody().get("code").asText();

        String stranger = newCustomer(uniqueEmail(), "secret-123");
        assertEquals(HttpStatus.NOT_FOUND, get("/api/orders/" + code, stranger).getStatusCode(), "IDOR: another customer");
        assertEquals(HttpStatus.NOT_FOUND, post("/api/orders/" + code + "/cancel", null, stranger).getStatusCode());

        String otherStore = login(storeWithProduct(20)[2], "store-pass-123");
        assertEquals(HttpStatus.NOT_FOUND, get("/api/orders/" + code, otherStore).getStatusCode(), "IDOR: another store");

        String ownStore = login(store[2], "store-pass-123");
        assertEquals(HttpStatus.OK, get("/api/orders/" + code, ownStore).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, get("/api/orders/" + code, null).getStatusCode());
    }

    @Test
    void onlyCustomersPlaceOrders() {
        String[] store = storeWithProduct(20);
        String staff = login(store[2], "store-pass-123");
        assertEquals(HttpStatus.FORBIDDEN, post("/api/orders", order(store[1], 1), staff).getStatusCode());
    }

    @Test
    void zonesArePublicAndManagedByAdmins() {
        JsonNode zones = get("/api/zones", null).getBody();
        assertEquals("SAMANNOUD", zones.get(0).get("code").asText());

        String customer = newCustomer(uniqueEmail(), "secret-123");
        Map<String, Object> zone = Map.of("code", "TEST_VILLAGE_" + System.nanoTime() % 100000, "nameAr", "قرية تجربة",
                "nameEn", "Test village", "kind", "VILLAGE", "fee", 25, "etaMinutes", 35, "sortOrder", 5);
        assertEquals(HttpStatus.FORBIDDEN, post("/api/admin/zones", zone, customer).getStatusCode());
        assertEquals(HttpStatus.CREATED, post("/api/admin/zones", zone, login(ADMIN_EMAIL, ADMIN_PASSWORD)).getStatusCode());
        assertNotNull(db.queryForObject("SELECT fee FROM delivery_zones WHERE code = ?", Object.class, zone.get("code")));
    }
}
