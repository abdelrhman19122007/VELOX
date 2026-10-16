package com.velox.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.JsonNode;

/** Admin orders, numbers and complaints; customer reviews and complaints. */
class AdminAndFeedbackApiTest extends OperationsApiTestBase {

    @Test
    void adminCancelsAnyOrderWithAReason() {
        String[] store = store();
        String customer = customer();
        String code = placeCash(customer, store[1]);
        act("/api/restaurant/orders/" + code + "/accept", store[2]);
        String admin = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        assertEquals(HttpStatus.FORBIDDEN, post("/api/admin/orders/" + code + "/cancel", Map.of("reason", "x"), customer).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, post("/api/admin/orders/" + code + "/cancel", Map.of(), admin).getStatusCode());
        JsonNode cancelled = post("/api/admin/orders/" + code + "/cancel", Map.of("reason", "العميل طلب بالتليفون"), admin).getBody();
        assertEquals("CANCELLED", cancelled.get("status").asText());
        assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM audit_log WHERE action = 'ORDER_CANCEL' AND entity_id = ?",
                Integer.class, code), "audited");
        assertTrue(contains(get("/api/admin/orders?status=CANCELLED", admin).getBody(), code));
        assertEquals(HttpStatus.BAD_REQUEST, get("/api/admin/orders?status=DROP TABLE", admin).getStatusCode());
    }

    @Test
    void adminNumbers() {
        String admin = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        String[] store = store();
        deliver(store[2], placeCash(customer(), store[1]));
        JsonNode stats = get("/api/admin/stats", admin).getBody();
        assertTrue(stats.get("deliveredToday").asInt() >= 1);
        assertTrue(stats.get("commissionToday").asDouble() >= 10.0, "10% of 100");
        assertEquals(HttpStatus.FORBIDDEN, get("/api/admin/stats", customer()).getStatusCode());
    }

    @Test
    void reviewOwnDeliveredOrderOnce() {
        String[] store = store();
        String customer = customer();
        String code = placeCash(customer, store[1]);
        Map<String, Object> review = Map.of("storeRating", 5, "driverRating", 4, "comment", "سخن وفي ميعاده");
        assertEquals("NOT_DELIVERED", post("/api/orders/" + code + "/review", review, customer).getBody().get("code").asText());

        deliver(store[2], code);
        assertEquals(HttpStatus.NOT_FOUND, post("/api/orders/" + code + "/review", review, customer()).getStatusCode(), "not my order");
        assertEquals(HttpStatus.BAD_REQUEST, post("/api/orders/" + code + "/review", Map.of("storeRating", 6), customer).getStatusCode());
        assertEquals(HttpStatus.CREATED, post("/api/orders/" + code + "/review", review, customer).getStatusCode());
        assertEquals("ALREADY_REVIEWED", post("/api/orders/" + code + "/review", review, customer).getBody().get("code").asText());

        JsonNode reviews = get("/api/restaurant/reviews", store[2]).getBody();
        assertEquals(5, reviews.get(0).get("storeRating").asInt());
        assertEquals(5.0, get("/api/stores/" + store[0], null).getBody().get("store").get("rating").asDouble(), "the store's rating follows");
    }

    @Test
    void complaintClosedWithAPersonalCompensation() {
        String[] store = store();
        String customer = customer();
        String code = placeCash(customer, store[1]);
        assertEquals(HttpStatus.BAD_REQUEST, post("/api/orders/" + code + "/complaint",
                Map.of("details", "<script>alert(1)</script>"), customer).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, post("/api/orders/" + code + "/complaint",
                Map.of("details", "الأكل وصل بارد"), customer()).getStatusCode());
        String complaint = post("/api/orders/" + code + "/complaint", Map.of("details", "الأكل وصل بارد"), customer)
                .getBody().get("code").asText();
        assertEquals("COMPLAINT_OPEN", post("/api/orders/" + code + "/complaint", Map.of("details", "تاني مرة بارد"), customer)
                .getBody().get("code").asText());

        String admin = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        assertEquals(HttpStatus.FORBIDDEN, get("/api/admin/complaints", customer).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, post("/api/admin/complaints/" + complaint + "/close",
                Map.of("compensation", 5000), admin).getStatusCode(), "max 200");
        JsonNode closed = post("/api/admin/complaints/" + complaint + "/close", Map.of("note", "اعتذرنا", "compensation", 50), admin).getBody();
        assertEquals("CLOSED", closed.get("status").asText());
        String promo = closed.get("compensationCode").asText();
        assertEquals("COMPLAINT_CLOSED", post("/api/admin/complaints/" + complaint + "/close",
                Map.of("compensation", 50), admin).getBody().get("code").asText(), "no second compensation");

        Map<String, Object> next = order(store[1], "CASH");
        next.put("promo", promo);
        assertEquals("INVALID_PROMO", post("/api/orders", next, customer()).getBody().get("code").asText(), "only for that customer");
        assertEquals(50.0, post("/api/orders", next, customer).getBody().get("discount").asDouble());
        assertEquals("CLOSED", get("/api/me/complaints", customer).getBody().get(0).get("status").asText());
    }
}
