package com.app.service;

import java.util.Optional;

/**
 * Feature 2 (instant wallet refund): pure compensation rules.
 * Only delivered orders qualify; one refund per order (enforced in DB).
 */
public final class RefundPolicy {

    /** Late-delivery threshold in minutes (order placed -> delivered). */
    public static final long LATE_THRESHOLD_MINUTES = 45;
    /** Late payout: 20% of the order total. */
    public static final double LATE_RATE = 0.20;
    /** Late payout cap in EGP. */
    public static final double LATE_CAP = 50.0;
    /** Wrong/damaged payout: 15% of the order total. */
    public static final double DAMAGE_RATE = 0.15;
    /** Wrong/damaged payout cap in EGP. */
    public static final double DAMAGE_CAP = 30.0;

    private RefundPolicy() {
    }

    /**
     * @param status           current order status (must be DELIVERED)
     * @param minutesSinceOrder minutes from order_date to delivered_at (or now)
     * @param total            order final_amount
     * @param reason           LATE, WRONG_ITEM or DAMAGED (case-insensitive)
     * @return payout amount, or empty when not eligible
     */
    public static Optional<Double> decide(String status, long minutesSinceOrder,
                                          double total, String reason) {
        if (status == null || !"DELIVERED".equalsIgnoreCase(status.trim())) {
            return Optional.empty();
        }
        if (total <= 0 || reason == null) {
            return Optional.empty();
        }
        String r = reason.trim().toUpperCase();
        switch (r) {
            case "LATE":
                if (minutesSinceOrder > LATE_THRESHOLD_MINUTES) {
                    return Optional.of(Math.min(total * LATE_RATE, LATE_CAP));
                }
                return Optional.empty();
            case "WRONG_ITEM":
            case "DAMAGED":
                return Optional.of(Math.min(total * DAMAGE_RATE, DAMAGE_CAP));
            default:
                return Optional.empty();
        }
    }
}
