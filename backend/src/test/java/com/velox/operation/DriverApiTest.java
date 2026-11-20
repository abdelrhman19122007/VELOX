package com.velox.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.JsonNode;

/** The driver app: offers, taking (races, one at a time, vehicles), the delivery steps, errands, location. */
class DriverApiTest extends OperationsApiTestBase {

    @Test
    void fromOfferToDoor() {
        String[] store = store();
        String customer = customer();
        String code = placeCash(customer, store[1]);
        String driver = onlineDriver("MOTORBIKE");
        assertFalse(contains(get("/api/driver/offers", driver).getBody(), code), "not before the store accepts");

        act("/api/restaurant/orders/" + code + "/accept", store[2]);
        assertTrue(contains(get("/api/driver/offers", driver).getBody(), code));
        assertEquals("ACCEPTED", act("/api/driver/orders/" + code + "/take", driver).get("status").asText());
        assertEquals("ACCEPTED", get("/api/orders/" + code, customer).getBody().get("status").asText());
        assertEquals("Driver", get("/api/orders/" + code, customer).getBody().get("driver").get("name").asText());

        assertEquals("BAD_STATUS", act("/api/driver/orders/" + code + "/pickup", driver).get("code").asText(), "not ready yet");
        act("/api/restaurant/orders/" + code + "/ready", store[2]);
        assertEquals("PICKED_UP", act("/api/driver/orders/" + code + "/pickup", driver).get("status").asText());
        assertEquals("DELIVERED", act("/api/driver/orders/" + code + "/deliver", driver).get("status").asText());

        JsonNode earnings = get("/api/driver/earnings", driver).getBody();
        assertEquals(1, earnings.get("deliveredToday").asInt());
        assertEquals(15.0, earnings.get("feesToday").asDouble());
        assertTrue(contains(get("/api/driver/orders?active=false", driver).getBody(), code));
    }

    @Test
    void offlineDriversGetNothing() {
        String driver = onlineDriver("MOTORBIKE");
        post("/api/driver/status", Map.of("online", false), driver);
        assertEquals("NOT_ONLINE", get("/api/driver/offers", driver).getBody().get("code").asText());
        assertEquals("MOTORBIKE", get("/api/driver/status", driver).getBody().get("vehicle").asText(), "vehicle kept");
        assertEquals(HttpStatus.FORBIDDEN, get("/api/driver/offers", customer()).getStatusCode());
    }

    @Test
    void onlyOneDriverGetsAnOrder() {
        String[] store = store();
        String code = placeCash(customer(), store[1]);
        act("/api/restaurant/orders/" + code + "/accept", store[2]);
        String first = onlineDriver("MOTORBIKE");
        String second = onlineDriver("MOTORBIKE");
        assertEquals(HttpStatus.OK, post("/api/driver/orders/" + code + "/take", null, first).getStatusCode());
        assertEquals("ALREADY_TAKEN", act("/api/driver/orders/" + code + "/take", second).get("code").asText());
        assertFalse(contains(get("/api/driver/offers", second).getBody(), code), "gone from the offers");

        act("/api/restaurant/orders/" + code + "/ready", store[2]);
        assertEquals(HttpStatus.NOT_FOUND, post("/api/driver/orders/" + code + "/pickup", null, second).getStatusCode(),
                "only the assigned driver");
        assertEquals(HttpStatus.NOT_FOUND, post("/api/driver/orders/" + code + "/deliver", null, second).getStatusCode());
    }

    @Test
    void oneOrderAtATime() {
        String[] store = store();
        String a = placeCash(customer(), store[1]);
        String b = placeCash(customer(), store[1]);
        act("/api/restaurant/orders/" + a + "/accept", store[2]);
        act("/api/restaurant/orders/" + b + "/accept", store[2]);
        String driver = onlineDriver("MOTORBIKE");
        act("/api/driver/orders/" + a + "/take", driver);
        assertEquals("BUSY", act("/api/driver/orders/" + b + "/take", driver).get("code").asText());
    }

    @Test
    void largeItemsNeedATuktuk() {
        String[] store = store(150, "LARGE");
        String code = placeCash(customer(), store[1]);
        act("/api/restaurant/orders/" + code + "/accept", store[2]);
        String bike = onlineDriver("MOTORBIKE");
        assertFalse(contains(get("/api/driver/offers", bike).getBody(), code));
        assertEquals("NEEDS_TUKTUK", act("/api/driver/orders/" + code + "/take", bike).get("code").asText());
        String tuktuk = onlineDriver("TUKTUK");
        assertTrue(contains(get("/api/driver/offers", tuktuk).getBody(), code));
        assertEquals(HttpStatus.OK, post("/api/driver/orders/" + code + "/take", null, tuktuk).getStatusCode());
    }

    @Test
    void errandsGoStraightToDrivers() {
        String customer = customer();
        Map<String, Object> errand = Map.of("kind", "ERRAND", "zone", "SAMANNOUD", "phone", "01012345678",
                "address", "شارع البحر، سمنود", "errandDetails", "علبة بنادول من أي صيدلية", "items", List.of(), "paymentMethod", "CASH");
        String code = post("/api/orders", errand, customer).getBody().get("code").asText();
        String driver = onlineDriver("MOTORBIKE");
        assertTrue(contains(get("/api/driver/offers", driver).getBody(), code));
        assertEquals("ACCEPTED", act("/api/driver/orders/" + code + "/take", driver).get("status").asText());
        assertEquals("PICKED_UP", act("/api/driver/orders/" + code + "/pickup", driver).get("status").asText(), "bought it");
        assertEquals("DELIVERED", act("/api/driver/orders/" + code + "/deliver", driver).get("status").asText());
    }

    @Test
    void locationIsCheckedAndShownToTheCustomer() {
        String token = newDriver();
        assertEquals("NOT_ONLINE", post("/api/driver/location", Map.of("lat", 31.0, "lng", 31.2), token).getBody().get("code").asText());
        post("/api/driver/status", Map.of("online", true), token);
        assertEquals(HttpStatus.NO_CONTENT, post("/api/driver/location", Map.of("lat", 31.0, "lng", 31.2), token).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, post("/api/driver/location", Map.of("lat", 200, "lng", 31.2), token).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, post("/api/driver/status", Map.of("online", true, "vehicle", "PLANE"), token).getStatusCode());
        assertEquals(31.0, get("/api/driver/status", token).getBody().get("lat").asDouble());
    }
}
