package com.app.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Feature 2: instant-refund compensation rules.
 */
class RefundPolicyTest {

    @Test
    void lateDeliveredOrderPaysTwentyPercentCapped() {
        assertEquals(40.0, RefundPolicy.decide("DELIVERED", 60, 200.0, "LATE").orElseThrow());
        assertEquals(50.0, RefundPolicy.decide("DELIVERED", 120, 1000.0, "LATE").orElseThrow());
    }

    @Test
    void onTimeDeliveryIsNotEligible() {
        assertTrue(RefundPolicy.decide("DELIVERED", 30, 200.0, "LATE").isEmpty());
        assertTrue(RefundPolicy.decide("DELIVERED", 45, 200.0, "LATE").isEmpty());
    }

    @Test
    void wrongOrDamagedPaysFifteenPercentCapped() {
        assertEquals(30.0, RefundPolicy.decide("DELIVERED", 20, 200.0, "WRONG_ITEM").orElseThrow());
        assertEquals(30.0, RefundPolicy.decide("DELIVERED", 20, 500.0, "DAMAGED").orElseThrow());
        assertEquals(15.0, RefundPolicy.decide("DELIVERED", 20, 100.0, "damaged").orElseThrow());
    }

    @Test
    void nonDeliveredOrUnknownReasonsRejected() {
        assertTrue(RefundPolicy.decide("PROCESSING", 999, 200.0, "LATE").isEmpty());
        assertTrue(RefundPolicy.decide("DELIVERED", 999, 200.0, "OTHER").isEmpty());
        assertTrue(RefundPolicy.decide("DELIVERED", 999, 200.0, null).isEmpty());
        assertTrue(RefundPolicy.decide(null, 999, 200.0, "LATE").isEmpty());
        assertTrue(RefundPolicy.decide("DELIVERED", 999, 0.0, "LATE").isEmpty());
    }
}
