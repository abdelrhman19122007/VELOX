package com.velox.payment.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;

import org.springframework.stereotype.Service;

import com.velox.common.error.ApiException;
import com.velox.order.spi.DiscountPolicy;
import com.velox.payment.repository.PromoRepository;
import com.velox.payment.repository.PromoRepository.Promo;
import com.velox.security.AuthUser;

/**
 * Promo codes (FR-PAY-08, Strategy behind {@link DiscountPolicy}): percent off the items, a fixed amount, or free
 * delivery — with a minimum order, an expiry, a total use limit, one use per customer, and personal codes
 * (compensation) that only their owner can use. Every failure gives the same INVALID_PROMO, so codes can't be probed.
 */
@Service
public class PromoDiscountPolicy implements DiscountPolicy {

    private final PromoRepository promos;
    private final Clock clock;

    public PromoDiscountPolicy(PromoRepository promos, Clock clock) {
        this.promos = promos;
        this.clock = clock;
    }

    @Override
    public Discount apply(AuthUser customer, String code, BigDecimal subtotal, BigDecimal deliveryFee) {
        Promo p = promos.find(code).orElseThrow(PromoDiscountPolicy::invalid);
        boolean ok = (p.expiresAt() == null || p.expiresAt().isAfter(clock.instant()))
                && (p.maxUses() == null || p.usedCount() < p.maxUses())
                && subtotal.compareTo(p.minOrder()) >= 0
                && (p.userId() == null || (customer != null && p.userId() == customer.id()))
                && (customer == null || !promos.usedBy(code, customer.id()));
        if (!ok) {
            throw invalid();
        }
        return switch (p.kind()) {
            case "PERCENT" -> new Discount(subtotal.multiply(p.value()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO);
            case "FIXED" -> new Discount(p.value().min(subtotal), BigDecimal.ZERO);
            case "FREE_DELIVERY" -> new Discount(BigDecimal.ZERO, deliveryFee);
            default -> throw invalid();
        };
    }

    @Override
    public void consume(AuthUser customer, String code, long orderId) {
        if (!promos.use(code)) {
            throw invalid();   // the last use was taken a moment ago — the order rolls back
        }
    }

    private static ApiException invalid() {
        return ApiException.badRequest("INVALID_PROMO", "This promo code can't be used");
    }
}
