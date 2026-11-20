package com.velox.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.JsonNode;

/** The inbox and push devices. Push itself stays off in tests (no Firebase files). */
class NotificationApiTest extends PaymentApiTestBase {

    @Test
    void storeHearsOfPaidOrdersAndCustomerOfChanges() {
        String[] store = storeWithProduct(40);
        String staff = login(store[2], "store-pass-123");
        String customer = newCustomer(uniqueEmail(), "secret-123");
        String code = place(customer, order(store[1], "FAWRY")).get("code").asText();
        assertEquals(0, get("/api/me/notifications", staff).getBody().size(), "nothing until it's paid");

        post("/api/orders/" + code + "/payment/sandbox-pay", null, customer);
        JsonNode inbox = get("/api/me/notifications", staff).getBody();
        assertEquals(1, inbox.size());
        assertTrue(inbox.get(0).get("title").asText().contains(code));
        assertFalse(inbox.get(0).get("read").asBoolean());

        post("/api/orders/" + code + "/cancel", null, customer);
        JsonNode mine = get("/api/me/notifications", customer).getBody();
        assertEquals(code, mine.get(0).get("orderCode").asText());

        assertEquals(HttpStatus.NO_CONTENT, post("/api/me/notifications/read", null, customer).getStatusCode());
        assertTrue(get("/api/me/notifications", customer).getBody().get(0).get("read").asBoolean());
        assertFalse(get("/api/me/notifications", staff).getBody().get(0).get("read").asBoolean(), "only my own inbox");
    }

    @Test
    void devicesBelongToTheirAccount() {
        String customer = newCustomer(uniqueEmail(), "secret-123");
        String other = newCustomer(uniqueEmail(), "secret-123");
        Map<String, Object> device = Map.of("token", "test-device-" + System.nanoTime(), "platform", "android", "lang", "ar");
        assertEquals(HttpStatus.NO_CONTENT, post("/api/me/devices", device, customer).getStatusCode());
        assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM push_devices WHERE token = ?", Integer.class, device.get("token")));

        post("/api/me/devices/remove", device, other);
        assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM push_devices WHERE token = ?", Integer.class, device.get("token")),
                "someone else can't remove my device");
        post("/api/me/devices/remove", device, customer);
        assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM push_devices WHERE token = ?", Integer.class, device.get("token")));

        assertEquals(HttpStatus.BAD_REQUEST, post("/api/me/devices", Map.of("token", "x".repeat(500)), customer).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, post("/api/me/devices", device, null).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, get("/api/me/notifications", null).getStatusCode());
    }

    @Test
    void pushConfigIsEmptyWithoutFirebase() {
        assertEquals(0, get("/api/push-config", null).getBody().size());
    }
}
