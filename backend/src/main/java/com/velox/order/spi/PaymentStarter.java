package com.velox.order.spi;

import com.velox.order.domain.Order;
import com.velox.order.domain.PaymentMethod;

/**
 * Starts an online payment for a just-placed order (Strategy: one implementation per method — Vodafone Cash,
 * Fawry, wallet — in the payments module). The order waits in PENDING_PAYMENT, hidden from the store, until the
 * payments module confirms it through {@code OrderLifecycle.markPaid}.
 */
public interface PaymentStarter {

    boolean supports(PaymentMethod method);

    /** {@code walletPhone} is only used by wallet-style methods (Vodafone Cash). */
    void start(Order order, String walletPhone);
}
