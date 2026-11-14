package com.velox.payment.service;

import java.math.BigDecimal;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.velox.common.error.ApiException;
import com.velox.order.domain.Order;
import com.velox.order.domain.OrderStatus;
import com.velox.order.domain.PaymentStatus;
import com.velox.order.repository.OrderRepository;
import com.velox.order.service.OrderLifecycle;
import com.velox.order.spi.OrderChanged;
import com.velox.payment.repository.PaymentRepository;
import com.velox.payment.repository.WalletRepository;

/**
 * Money follows the order (FR-PAY-04, -05). Listens to order changes inside the same transaction, so the status
 * change and the refund succeed or fail together:
 * <ul>
 *   <li>rejected / cancelled after paying → full refund to the wallet;</li>
 *   <li>returned → the items' value back (delivery isn't refunded);</li>
 *   <li>cancelled before paying → the open payment just expires.</li>
 * </ul>
 * Also cancels online orders whose time to pay ran out.
 */
@Component
public class RefundListener {

    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final WalletRepository wallet;
    private final OrderLifecycle lifecycle;
    private final java.time.Clock clock;

    public RefundListener(OrderRepository orders, PaymentRepository payments, WalletRepository wallet, OrderLifecycle lifecycle,
                          java.time.Clock clock) {
        this.orders = orders;
        this.payments = payments;
        this.wallet = wallet;
        this.lifecycle = lifecycle;
        this.clock = clock;
    }

    @EventListener
    public void onChange(OrderChanged change) {
        if (change.to() != OrderStatus.REJECTED && change.to() != OrderStatus.CANCELLED && change.to() != OrderStatus.RETURNED) {
            return;
        }
        Order o = orders.findById(change.orderId()).orElseThrow();
        if (o.paymentStatus() == PaymentStatus.PENDING) {
            payments.markExpired(o.id());
            orders.setPaymentStatus(o.id(), PaymentStatus.PENDING, PaymentStatus.EXPIRED);
        } else if (o.paymentStatus() == PaymentStatus.PAID) {
            BigDecimal amount = change.to() == OrderStatus.RETURNED ? o.subtotal().subtract(o.discount()).max(BigDecimal.ZERO) : o.total();
            payments.insertRefund(o.id(), o.paymentMethod(), amount);
            wallet.add(o.customerId(), amount, "REFUND", o.id());
            orders.setPaymentStatus(o.id(), PaymentStatus.PAID, PaymentStatus.REFUNDED);
        }
    }

    /** Online orders not paid in time (10 min wallet / 60 min Fawry) are cancelled. Checked every 30 s. */
    @Scheduled(fixedDelay = 30_000, initialDelay = 30_000)
    public void expireUnpaid() {
        for (long orderId : payments.expiredPendingOrders(clock.instant())) {
            try {
                lifecycle.move(orderId, OrderStatus.CANCELLED, null, "Payment time ran out");
            } catch (ApiException e) {
                payments.markExpired(orderId);   // paid or cancelled meanwhile
            }
        }
    }
}
