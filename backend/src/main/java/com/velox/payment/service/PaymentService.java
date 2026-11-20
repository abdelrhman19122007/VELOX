package com.velox.payment.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.velox.common.error.ApiException;
import com.velox.order.domain.Order;
import com.velox.order.domain.PaymentMethod;
import com.velox.order.repository.OrderRepository;
import com.velox.order.service.OrderLifecycle;
import com.velox.payment.domain.Payment;
import com.velox.payment.repository.PaymentRepository;
import com.velox.security.AuthUser;
import com.velox.security.PasswordHasher;
import com.velox.security.Role;

/**
 * Finishing an online payment (FR-PAY-02…04): the payment details, the Vodafone Cash code, and the sandbox
 * stand-in for Fawry's "paid" notification. Only the order's own customer can see or act on its payment.
 */
@Service
public class PaymentService {

    static final int MAX_CODE_ATTEMPTS = 5;

    /** What the customer needs to pay. {@code sandboxCode} only exists in sandbox mode (it replaces the SMS). */
    public record PaymentInfo(String orderCode, PaymentMethod method, String status, BigDecimal amount, String reference,
                              String walletPhone, Instant expiresAt, boolean sandbox, String sandboxCode) {
    }

    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final OrderLifecycle lifecycle;
    private final PasswordHasher hasher;
    private final Clock clock;
    private final boolean sandbox;

    public PaymentService(OrderRepository orders, PaymentRepository payments, OrderLifecycle lifecycle, PasswordHasher hasher,
                          Clock clock, @Value("${velox.payments.sandbox:true}") boolean sandbox) {
        this.orders = orders;
        this.payments = payments;
        this.lifecycle = lifecycle;
        this.hasher = hasher;
        this.clock = clock;
        this.sandbox = sandbox;
    }

    public PaymentInfo info(AuthUser customer, String orderCode) {
        Order o = own(customer, orderCode);
        Payment p = payments.latestCharge(o.id()).orElseThrow(() -> ApiException.notFound("NO_PAYMENT", "This order has no online payment"));
        String code = sandbox && "PENDING".equals(p.status()) ? SandboxCodes.peek(o.id()) : null;
        return new PaymentInfo(o.code(), p.method(), p.status(), p.amount(), p.reference(), p.walletPhone(), p.expiresAt(),
                sandbox, code);
    }

    /**
     * Vodafone Cash: the code from the SMS. 5 wrong tries close the payment. A wrong code must not roll back,
     * or the attempt counter would never grow and the code could be guessed.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public void confirmWallet(AuthUser customer, String orderCode, String typed) {
        Order o = own(customer, orderCode);
        Payment p = openCharge(o, PaymentMethod.VODAFONE_CASH);
        if (p.attempts() >= MAX_CODE_ATTEMPTS) {
            throw ApiException.conflict("TOO_MANY_ATTEMPTS", "Too many wrong codes — order again");
        }
        if (!hasher.matches(typed == null ? "" : typed.trim(), p.codeHash())) {
            payments.countWrongCode(p.id());
            throw ApiException.badRequest("WRONG_CODE", "Wrong confirmation code");
        }
        paid(o, p);
    }

    /** Sandbox only: stands in for Fawry telling us the reference was paid at an outlet. */
    @Transactional
    public void sandboxPay(AuthUser customer, String orderCode) {
        if (!sandbox) {
            throw ApiException.notFound("NOT_FOUND", "Not found");
        }
        Order o = own(customer, orderCode);
        paid(o, openCharge(o, PaymentMethod.FAWRY));
    }

    private void paid(Order o, Payment p) {
        if (!payments.markPaid(p.id())) {
            throw ApiException.conflict("PAYMENT_CLOSED", "This payment is already closed");
        }
        SandboxCodes.forget(o.id());
        lifecycle.markPaid(o.id());
    }

    private Payment openCharge(Order o, PaymentMethod expected) {
        Payment p = payments.pendingCharge(o.id())
                .orElseThrow(() -> ApiException.conflict("PAYMENT_CLOSED", "This payment is already closed"));
        if (p.method() != expected) {
            throw ApiException.badRequest("INVALID_REQUEST", "This order isn't paid that way");
        }
        if (p.expired(clock.instant())) {
            throw ApiException.conflict("PAYMENT_EXPIRED", "The time to pay is over — order again");
        }
        return p;
    }

    private Order own(AuthUser customer, String orderCode) {
        customer.require(Role.CUSTOMER);
        return orders.findByCode(orderCode == null ? "" : orderCode.trim().toUpperCase())
                .filter(o -> o.customerId() == customer.id())
                .orElseThrow(() -> ApiException.notFound("ORDER_NOT_FOUND", "Order not found"));
    }
}
