package com.app.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Feature 5: price-watch trigger rules.
 */
class PriceWatchMatcherTest {

    @Test
    void firesOnceOnDropBelowTarget() {
        assertTrue(PriceWatchMatcher.shouldNotify(90.0, 100.0, false));
        assertFalse(PriceWatchMatcher.shouldNotify(90.0, 100.0, true));
    }

    @Test
    void noFireWithoutRealDrop() {
        assertFalse(PriceWatchMatcher.shouldNotify(100.0, 100.0, false));
        assertFalse(PriceWatchMatcher.shouldNotify(120.0, 100.0, false));
        assertFalse(PriceWatchMatcher.shouldNotify(0.0, 100.0, false));
    }

    @Test
    void rearmsWhenPriceRecovers() {
        assertTrue(PriceWatchMatcher.shouldRearm(120.0, 100.0, true));
        assertFalse(PriceWatchMatcher.shouldRearm(90.0, 100.0, true));
        assertFalse(PriceWatchMatcher.shouldRearm(120.0, 100.0, false));
    }
}
