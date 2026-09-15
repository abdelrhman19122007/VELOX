package com.app.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Feature 4: VELOX Plus subscription rules.
 */
class PlusServiceTest {

    @Test
    void activeWhileExpiryInFuture() {
        LocalDateTime now = LocalDateTime.now();
        assertTrue(PlusService.isActive(now.plusDays(1), now));
        assertFalse(PlusService.isActive(now.minusDays(1), now));
        assertFalse(PlusService.isActive(null, now));
        assertFalse(PlusService.isActive(now.plusDays(1), null));
    }

    @Test
    void planPricingConstants() {
        assertEquals(50.0, PlusService.MONTHLY_PRICE);
        assertEquals(30, PlusService.DURATION_DAYS);
        assertEquals("PLUS_MONTHLY", PlusService.PLAN);
    }
}
