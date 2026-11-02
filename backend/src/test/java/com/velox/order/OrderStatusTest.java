package com.velox.order;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.velox.order.domain.OrderStatus;
import com.velox.order.domain.OrderStatus.Actor;

/** Unit tests of the order state machine: each move only by the right kind of account. */
class OrderStatusTest {

    @Test
    void happyPath() {
        assertTrue(OrderStatus.PLACED.canMoveTo(OrderStatus.ACCEPTED, Actor.RESTAURANT));
        assertTrue(OrderStatus.ACCEPTED.canMoveTo(OrderStatus.READY, Actor.RESTAURANT));
        assertTrue(OrderStatus.READY.canMoveTo(OrderStatus.PICKED_UP, Actor.DRIVER));
        assertTrue(OrderStatus.PICKED_UP.canMoveTo(OrderStatus.DELIVERED, Actor.DRIVER));
        assertTrue(OrderStatus.DELIVERED.canMoveTo(OrderStatus.RETURNED, Actor.CUSTOMER));
    }

    @Test
    void wrongActorsAreRefused() {
        assertFalse(OrderStatus.PLACED.canMoveTo(OrderStatus.ACCEPTED, Actor.CUSTOMER), "customer can't accept own order");
        assertFalse(OrderStatus.PLACED.canMoveTo(OrderStatus.ACCEPTED, Actor.DRIVER));
        assertFalse(OrderStatus.READY.canMoveTo(OrderStatus.PICKED_UP, Actor.RESTAURANT));
        assertFalse(OrderStatus.PICKED_UP.canMoveTo(OrderStatus.DELIVERED, Actor.CUSTOMER));
        assertFalse(OrderStatus.PENDING_PAYMENT.canMoveTo(OrderStatus.PLACED, Actor.CUSTOMER), "only a confirmed payment");
    }

    @Test
    void stepsCannotBeSkippedOrUndone() {
        assertFalse(OrderStatus.PLACED.canMoveTo(OrderStatus.DELIVERED, Actor.DRIVER));
        assertFalse(OrderStatus.ACCEPTED.canMoveTo(OrderStatus.PICKED_UP, Actor.DRIVER), "must be READY first");
        assertFalse(OrderStatus.DELIVERED.canMoveTo(OrderStatus.PLACED, Actor.ADMIN));
        assertFalse(OrderStatus.ACCEPTED.canMoveTo(OrderStatus.CANCELLED, Actor.CUSTOMER), "too late for the customer");
        assertTrue(OrderStatus.CANCELLED.isFinal());
        assertTrue(OrderStatus.REJECTED.isFinal());
    }
}
