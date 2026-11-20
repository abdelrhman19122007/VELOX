package com.velox.payment.gateway;

import java.time.Duration;

import com.velox.order.domain.Order;
import com.velox.order.domain.PaymentMethod;

/**
 * A payment provider (Strategy): one implementation per method. Today the sandbox implementations stand in for
 * Vodafone Cash and Fawry; with a merchant account, a real adapter replaces them and nothing else changes.
 */
public interface PaymentGateway {

    /** What the provider gave back when the payment was opened. {@code confirmationCode} is the SMS code (wallets). */
    record Started(String reference, String confirmationCode, Duration validFor) {
    }

    PaymentMethod method();

    Started start(Order order, String walletPhone);
}
