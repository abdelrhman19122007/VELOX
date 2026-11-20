package com.velox.payment.gateway;

import java.security.SecureRandom;
import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.velox.order.domain.Order;
import com.velox.order.domain.PaymentMethod;

/**
 * Sandbox Vodafone Cash: instead of sending an SMS to the wallet, the confirmation code is shown in the app.
 * Active while {@code velox.payments.sandbox=true} (the default until a merchant account is connected).
 */
@Component
@ConditionalOnProperty(name = "velox.payments.sandbox", havingValue = "true", matchIfMissing = true)
public class SandboxVodafoneCash implements PaymentGateway {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public PaymentMethod method() {
        return PaymentMethod.VODAFONE_CASH;
    }

    @Override
    public Started start(Order order, String walletPhone) {
        String reference = "VC" + (1_000_000_000L + (long) (RANDOM.nextDouble() * 8_999_999_999L));
        String code = String.valueOf(100_000 + RANDOM.nextInt(900_000));
        return new Started(reference, code, Duration.ofMinutes(10));
    }
}
