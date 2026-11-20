package com.velox.payment.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.velox.common.error.ApiException;
import com.velox.order.domain.Order;
import com.velox.order.domain.PaymentMethod;
import com.velox.order.service.OrderLifecycle;
import com.velox.order.spi.PaymentStarter;
import com.velox.payment.repository.PaymentRepository;
import com.velox.payment.repository.WalletRepository;

/**
 * Pays from the in-app wallet at once (FR-PAY-07). The wallet is locked for the transaction, so two orders placed
 * at the same moment can't spend the same balance; not enough money rolls the whole order back.
 */
@Service
public class WalletPaymentStarter implements PaymentStarter {

    private final WalletRepository wallet;
    private final PaymentRepository payments;
    private final OrderLifecycle lifecycle;

    public WalletPaymentStarter(WalletRepository wallet, PaymentRepository payments, OrderLifecycle lifecycle) {
        this.wallet = wallet;
        this.payments = payments;
        this.lifecycle = lifecycle;
    }

    @Override
    public boolean supports(PaymentMethod method) {
        return method == PaymentMethod.WALLET;
    }

    @Override
    public void start(Order order, String walletPhone) {
        BigDecimal balance = wallet.balanceForUpdate(order.customerId());
        if (balance.compareTo(order.total()) < 0) {
            throw ApiException.conflict("INSUFFICIENT_BALANCE", "Not enough money in the wallet");
        }
        wallet.add(order.customerId(), order.total().negate(), "ORDER", order.id());
        payments.insertCharge(order.id(), PaymentMethod.WALLET, order.total(), "PAID", null, null, null, null);
        lifecycle.markPaid(order.id());
    }
}
