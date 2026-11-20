package com.velox.payment.gateway;

import java.security.SecureRandom;
import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.velox.order.domain.Order;
import com.velox.order.domain.PaymentMethod;

/**
 * Sandbox Fawry: hands out a 9-digit reference to pay at any Fawry outlet or in myFawry. The "paid at the outlet"
 * notification is simulated by a sandbox-only endpoint.
 */
@Component
@ConditionalOnProperty(name = "velox.payments.sandbox", havingValue = "true", matchIfMissing = true)
public class SandboxFawry implements PaymentGateway {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public PaymentMethod method() {
        return PaymentMethod.FAWRY;
    }

    @Override
    public Started start(Order order, String walletPhone) {
        return new Started(String.valueOf(100_000_000 + RANDOM.nextInt(900_000_000)), null, Duration.ofMinutes(60));
    }
}
