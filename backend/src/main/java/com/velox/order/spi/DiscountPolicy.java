package com.velox.order.spi;

import java.math.BigDecimal;

import com.velox.security.AuthUser;

/**
 * How a promo code changes an order's price (Strategy). The orders module only knows this interface;
 * the payments module provides the real promo-code rules. Without one, codes are refused.
 */
public interface DiscountPolicy {

    /** The result of applying a code: how much to take off the items and the delivery. */
    record Discount(BigDecimal itemsOff, BigDecimal deliveryOff) {
        public static final Discount NONE = new Discount(BigDecimal.ZERO, BigDecimal.ZERO);

        public BigDecimal total() {
            return itemsOff.add(deliveryOff);
        }
    }

    /** Throws an ApiException (e.g. INVALID_PROMO) when the code can't be used. */
    Discount apply(AuthUser customer, String code, BigDecimal subtotal, BigDecimal deliveryFee);

    /** Called once the order is saved, so a limited code is counted as used. */
    default void consume(AuthUser customer, String code, long orderId) {
    }
}
