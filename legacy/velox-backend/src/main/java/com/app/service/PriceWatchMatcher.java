package com.app.service;

/**
 * Feature 5 (price watch): pure trigger rule.
 * A watch fires once when the live price drops below the target;
 * it re-arms automatically once the price climbs back up.
 */
public final class PriceWatchMatcher {

    private PriceWatchMatcher() {
    }

    public static boolean shouldNotify(double currentPrice, double targetPrice, boolean alreadyNotified) {
        return !alreadyNotified && currentPrice > 0 && targetPrice > 0 && currentPrice < targetPrice;
    }

    public static boolean shouldRearm(double currentPrice, double targetPrice, boolean alreadyNotified) {
        return alreadyNotified && currentPrice >= targetPrice;
    }
}
