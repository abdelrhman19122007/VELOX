package com.velox.payment.service;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.velox.common.error.ApiException;
import com.velox.order.domain.Order;
import com.velox.order.domain.PaymentMethod;
import com.velox.order.spi.PaymentStarter;
import com.velox.payment.gateway.PaymentGateway;
import com.velox.payment.repository.PaymentRepository;
import com.velox.security.PasswordHasher;

/**
 * Opens a Vodafone Cash or Fawry payment for a just-placed order (FR-PAY-02, -03): asks the gateway for a
 * reference (and the SMS code for wallets) and records the pending charge. The code is stored hashed.
 */
@Service
public class OnlinePaymentStarter implements PaymentStarter {

    private final List<PaymentGateway> gateways;
    private final PaymentRepository payments;
    private final PasswordHasher hasher;
    private final Clock clock;

    public OnlinePaymentStarter(List<PaymentGateway> gateways, PaymentRepository payments, PasswordHasher hasher, Clock clock) {
        this.gateways = gateways;
        this.payments = payments;
        this.hasher = hasher;
        this.clock = clock;
    }

    @Override
    public boolean supports(PaymentMethod method) {
        return gateway(method).isPresent();
    }

    @Override
    public void start(Order order, String walletPhone) {
        PaymentGateway gateway = gateway(order.paymentMethod()).orElseThrow();
        if (order.paymentMethod() == PaymentMethod.VODAFONE_CASH && (walletPhone == null || walletPhone.isBlank())) {
            throw ApiException.badRequest("INVALID_WALLET", "Enter a Vodafone Cash number (010…)");
        }
        PaymentGateway.Started s = gateway.start(order, walletPhone);
        payments.insertCharge(order.id(), order.paymentMethod(), order.total(), "PENDING", s.reference(), walletPhone,
                s.confirmationCode() == null ? null : hasher.hash(s.confirmationCode()),
                clock.instant().plus(s.validFor()));
        SandboxCodes.remember(order.id(), s.confirmationCode());
    }

    private Optional<PaymentGateway> gateway(PaymentMethod method) {
        return gateways.stream().filter(g -> g.method() == method).findFirst();
    }
}
