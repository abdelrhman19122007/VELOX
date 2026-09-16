package com.app.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coupon validation rules + math.
 */
class PromoServiceTest {

    private static PromoService.Coupon coupon(double percent, double min, Integer max, int used,
                                              boolean freeShip, LocalDateTime exp) {
        return new PromoService.Coupon("TEST", percent, min, max, used, freeShip, exp);
    }

    @Test
    void rejectsUnknownExpiredExhaustedAndSmallOrders() {
        LocalDateTime now = LocalDateTime.now();
        assertTrue(PromoService.rejectionReason(null, 500, now).isPresent());
        assertTrue(PromoService.rejectionReason(
                coupon(20, 0, null, 0, false, now.minusDays(1)), 500, now).isPresent());
        assertTrue(PromoService.rejectionReason(
                coupon(20, 0, 10, 10, false, null), 500, now).isPresent());
        assertTrue(PromoService.rejectionReason(
                coupon(20, 500, null, 0, false, null), 100, now).isPresent());
    }

    @Test
    void acceptsValidCoupon() {
        LocalDateTime now = LocalDateTime.now();
        assertTrue(PromoService.rejectionReason(
                coupon(25, 300, 500, 0, false, null), 400, now).isEmpty());
    }

    @Test
    void discountMathCapsAtSubtotal() {
        PromoService.Applied a = PromoService.apply(coupon(50, 0, null, 0, false, null), 400);
        assertEquals(200.0, a.discount());
        assertFalse(a.freeShipping());
        PromoService.Applied b = PromoService.apply(coupon(150, 0, null, 0, false, null), 400);
        assertEquals(400.0, b.discount());
    }

    @Test
    void freeShippingFlagPassesThrough() {
        PromoService.Applied a = PromoService.apply(coupon(0, 150, null, 0, true, null), 200);
        assertEquals(0.0, a.discount());
        assertTrue(a.freeShipping());
    }
}
