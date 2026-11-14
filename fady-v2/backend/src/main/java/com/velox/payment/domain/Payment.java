package com.velox.payment.domain;

import java.math.BigDecimal;
import java.time.Instant;

import com.velox.order.domain.PaymentMethod;

/** One payment attempt or refund ({@code payments}). The confirmation code is only ever stored hashed. */
public record Payment(long id, long orderId, PaymentMethod method, String kind, BigDecimal amount, String status,
                      String reference, String walletPhone, String codeHash, int attempts, Instant expiresAt,
                      Instant paidAt) {

    public boolean expired(Instant now) {
        return expiresAt != null && expiresAt.isBefore(now);
    }
}
