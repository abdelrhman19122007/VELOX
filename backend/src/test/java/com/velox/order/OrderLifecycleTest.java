package com.velox.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.JsonNode;
import com.velox.common.error.ApiException;
import com.velox.order.domain.OrderStatus;
import com.velox.order.live.EventHub;
import com.velox.order.repository.OrderRepository;
import com.velox.order.service.OrderLifecycle;
import com.velox.order.service.StaleOrderCloser;
import com.velox.security.AuthUser;

/** The lifecycle as other modules use it, timeouts, returns and live updates. */
class OrderLifecycleTest extends OrderApiTestBase {

    @Autowired
    OrderLifecycle lifecycle;
    @Autowired
    OrderRepository orders;
    @Autowired
    StaleOrderCloser closer;
    @Autowired
    EventHub hub;

    private long idOf(String code) {
        return orders.findByCode(code).orElseThrow().id();
    }

    @Test
    void customerCancelsOnlyBeforeTheStoreAccepts() {
        String[] store = storeWithProduct(20);
        String customer = newCustomer(uniqueEmail(), "secret-123");
        String early = post("/api/orders", order(store[1], 1), customer).getBody().get("code").asText();
        assertEquals("CANCELLED", post("/api/orders/" + early + "/cancel", null, customer).getBody().get("status").asText());

        String late = post("/api/orders", order(store[1], 1), customer).getBody().get("code").asText();
        lifecycle.move(idOf(late), OrderStatus.ACCEPTED, account(store[2]), null);
        assertEquals("CANNOT_CANCEL", post("/api/orders/" + late + "/cancel", null, customer).getBody().get("code").asText());
    }

    @Test
    void onlyTheRightAccountMovesAnOrder() {
        String[] store = storeWithProduct(20);
        String customerEmail = uniqueEmail();
        String customer = newCustomer(customerEmail, "secret-123");
        long id = idOf(post("/api/orders", order(store[1], 1), customer).getBody().get("code").asText());

        AuthUser otherStore = account(storeWithProduct(20)[2]);
        ApiException e = assertThrows(ApiException.class, () -> lifecycle.move(id, OrderStatus.ACCEPTED, otherStore, null));
        assertEquals(HttpStatus.NOT_FOUND, e.status(), "another store can't accept it");

        AuthUser owner = account(customerEmail);
        assertEquals("BAD_STATUS", assertThrows(ApiException.class,
                () -> lifecycle.move(id, OrderStatus.ACCEPTED, owner, null)).code(), "the customer can't accept their own order");

        AuthUser staff = account(store[2]);
        assertEquals("BAD_STATUS", assertThrows(ApiException.class,
                () -> lifecycle.move(id, OrderStatus.DELIVERED, staff, null)).code(), "steps can't be skipped");

        lifecycle.move(id, OrderStatus.ACCEPTED, staff, null);
        lifecycle.move(id, OrderStatus.READY, staff, null);
        JsonNode view = get("/api/orders/" + orders.findById(id).orElseThrow().code(), customer).getBody();
        assertEquals("READY", view.get("status").asText());
        assertEquals(3, view.get("timeline").size());
    }

    @Test
    void ordersNobodyMovesOnAreClosed() {
        String[] store = storeWithProduct(20);
        String customer = newCustomer(uniqueEmail(), "secret-123");
        String code = post("/api/orders", order(store[1], 1), customer).getBody().get("code").asText();
        db.update("UPDATE orders SET updated_at = CURRENT_TIMESTAMP - INTERVAL '2' HOUR WHERE code = ?", code);

        closer.closeStale();
        JsonNode o = get("/api/orders/" + code, customer).getBody();
        assertEquals("CANCELLED", o.get("status").asText());
        assertEquals("The store didn't answer", o.get("rejectReason").asText());
    }

    @Test
    void returnOnlyWithinADayOfDelivery() {
        String[] store = storeWithProduct(20);
        String customer = newCustomer(uniqueEmail(), "secret-123");
        String code = post("/api/orders", order(store[1], 1), customer).getBody().get("code").asText();
        long id = idOf(code);
        assertEquals("RETURN_EXPIRED", post("/api/orders/" + code + "/return", null, customer).getBody().get("code").asText(),
                "not delivered yet");

        db.update("UPDATE orders SET status = 'DELIVERED' WHERE id = ?", id);
        assertEquals("RETURNED", post("/api/orders/" + code + "/return", null, customer).getBody().get("status").asText());
    }

    @Test
    void participantsGetLiveUpdates() {
        String[] store = storeWithProduct(20);
        String email = uniqueEmail();
        String customer = newCustomer(email, "secret-123");
        long customerId = account(email).id();
        long storeStaffId = account(store[2]).id();
        long start = ((EventHub.Poll) hub.poll(storeStaffId, null).getResult()).last();

        String code = post("/api/orders", order(store[1], 1), customer).getBody().get("code").asText();
        EventHub.Poll forStore = (EventHub.Poll) hub.poll(storeStaffId, start).getResult();
        assertTrue(forStore.events().stream().anyMatch(e -> e.event().equals("order")), "the store hears about the new order");
        assertTrue(((EventHub.Poll) hub.poll(customerId, start).getResult()).events().size() >= 1);

        long stranger = account(newCustomerEmail()).id();
        assertTrue(((EventHub.Poll) hub.poll(stranger, start).getResult()) == null
                || ((EventHub.Poll) hub.poll(stranger, start).getResult()).events().isEmpty(), "nobody else does: " + code);
    }

    private String newCustomerEmail() {
        String email = uniqueEmail();
        newCustomer(email, "secret-123");
        return email;
    }
}
