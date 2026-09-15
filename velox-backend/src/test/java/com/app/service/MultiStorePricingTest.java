package com.app.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Feature 1: multi-store pricing rules.
 */
class MultiStorePricingTest {

    @Test
    void singleStoreShipsAtBaseFeeOnly() {
        assertEquals(0.0, MultiStorePricing.extraFee(1));
        assertEquals(0.0, MultiStorePricing.extraFee(0));
    }

    @Test
    void eachExtraStoreAddsFlatFee() {
        assertEquals(10.0, MultiStorePricing.extraFee(2));
        assertEquals(20.0, MultiStorePricing.extraFee(3));
    }

    @Test
    void linesGroupByStoreInOrder() {
        var lines = List.of(
                new MultiStorePricing.StoreLine(5, "S5", 100.0),
                new MultiStorePricing.StoreLine(7, "S7", 50.0),
                new MultiStorePricing.StoreLine(5, "S5", 25.0));

        var groups = MultiStorePricing.groupByStore(lines);

        assertEquals(2, groups.size());
        assertEquals(5, groups.get(0).storeId());
        assertEquals("S5", groups.get(0).storeName());
        assertEquals(125.0, groups.get(0).subtotal());
        assertEquals(2, groups.get(0).lines());
        assertEquals(7, groups.get(1).storeId());
        assertEquals(50.0, groups.get(1).subtotal());
    }

    @Test
    void emptyCartGroupsToNothing() {
        assertTrue(MultiStorePricing.groupByStore(List.of()).isEmpty());
        assertTrue(MultiStorePricing.groupByStore(null).isEmpty());
    }
}
