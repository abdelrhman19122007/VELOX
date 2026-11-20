package com.velox.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.JsonNode;

/** The store dashboard: only its own paid orders, accept / reject / ready, numbers. */
class RestaurantOrderApiTest extends OperationsApiTestBase {

    @Test
    void storeSeesOnlyItsOwnPaidOrders() {
        String[] store = store();
        String customer = customer();
        String unpaid = post("/api/orders", order(store[1], "FAWRY"), customer).getBody().get("code").asText();
        assertFalse(contains(get("/api/restaurant/orders", store[2]).getBody(), unpaid), "unpaid orders stay hidden");
        assertEquals(HttpStatus.NOT_FOUND, post("/api/restaurant/orders/" + unpaid + "/accept", null, store[2]).getStatusCode());

        post("/api/orders/" + unpaid + "/payment/sandbox-pay", null, customer);
        assertTrue(contains(get("/api/restaurant/orders", store[2]).getBody(), unpaid));

        String[] otherStore = store();
        assertFalse(contains(get("/api/restaurant/orders", otherStore[2]).getBody(), unpaid));
        assertEquals(HttpStatus.NOT_FOUND, post("/api/restaurant/orders/" + unpaid + "/accept", null, otherStore[2]).getStatusCode(),
                "another store's order doesn't exist for me");
    }

    @Test
    void acceptThenReady() {
        String[] store = store();
        String code = placeCash(customer(), store[1]);
        assertEquals("ACCEPTED", act("/api/restaurant/orders/" + code + "/accept", store[2]).get("status").asText());
        assertEquals("BAD_STATUS", act("/api/restaurant/orders/" + code + "/accept", store[2]).get("code").asText(), "not twice");
        assertEquals("READY", act("/api/restaurant/orders/" + code + "/ready", store[2]).get("status").asText());
        assertEquals(HttpStatus.CONFLICT, post("/api/restaurant/orders/" + code + "/reject", Map.of("reason", "late"), store[2])
                .getStatusCode(), "too late to reject");
    }

    @Test
    void rejectNeedsAReasonAndRefundsOnlinePayments() {
        String[] store = store();
        String customer = customer();
        String code = post("/api/orders", order(store[1], "FAWRY"), customer).getBody().get("code").asText();
        post("/api/orders/" + code + "/payment/sandbox-pay", null, customer);

        assertEquals(HttpStatus.BAD_REQUEST, post("/api/restaurant/orders/" + code + "/reject", Map.of("reason", " "), store[2]).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, post("/api/restaurant/orders/" + code + "/reject",
                Map.of("reason", "<img src=x onerror=alert(1)>"), store[2]).getStatusCode());
        JsonNode rejected = post("/api/restaurant/orders/" + code + "/reject", Map.of("reason", "الصنف خلص"), store[2]).getBody();
        assertEquals("REJECTED", rejected.get("status").asText());
        assertEquals("الصنف خلص", get("/api/orders/" + code, customer).getBody().get("rejectReason").asText());
        assertEquals(115.0, get("/api/me/wallet", customer).getBody().get("balance").asDouble(), "paid online → back to the wallet");
    }

    @Test
    void todaysNumbers() {
        String[] store = store();
        String code = placeCash(customer(), store[1]);
        deliver(store[2], code);
        JsonNode stats = get("/api/restaurant/stats", store[2]).getBody();
        assertEquals(1, stats.get("ordersToday").asInt());
        assertEquals(1, stats.get("deliveredToday").asInt());
        assertEquals(100.0, stats.get("salesToday").asDouble(), "items only, not delivery");
    }

    @Test
    void onlyStoreStaff() {
        String customer = customer();
        assertEquals(HttpStatus.FORBIDDEN, get("/api/restaurant/orders", customer).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, get("/api/restaurant/stats", onlineDriver("MOTORBIKE")).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, get("/api/restaurant/orders", null).getStatusCode());
    }
}
