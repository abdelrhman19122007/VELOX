package com.velox.payment.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.velox.common.error.ApiException;
import com.velox.order.domain.OrderStatus;
import com.velox.order.repository.OrderRepository;
import com.velox.order.spi.OrderChanged;
import com.velox.payment.repository.LoyaltyRepository;
import com.velox.payment.repository.WalletRepository;
import com.velox.payment.repository.WalletRepository.Transaction;
import com.velox.security.AuthUser;
import com.velox.security.Role;

/** The customer's wallet and loyalty points (FR-PAY-07, -09). Points: 1 per 10 EGP of a delivered order. */
@Service
public class WalletService {

    static final BigDecimal MAX_SANDBOX_TOP_UP = new BigDecimal("1000");

    public record WalletView(BigDecimal balance, int points, List<Transaction> history) {
    }

    private final WalletRepository wallet;
    private final LoyaltyRepository loyalty;
    private final OrderRepository orders;
    private final boolean sandbox;

    public WalletService(WalletRepository wallet, LoyaltyRepository loyalty, OrderRepository orders,
                         @Value("${velox.payments.sandbox:true}") boolean sandbox) {
        this.wallet = wallet;
        this.loyalty = loyalty;
        this.orders = orders;
        this.sandbox = sandbox;
    }

    public WalletView view(AuthUser me) {
        me.require(Role.CUSTOMER);
        return new WalletView(wallet.balance(me.id()), loyalty.balance(me.id()), wallet.history(me.id()));
    }

    /** Sandbox only: add test money (a real top-up goes through Fawry / Vodafone Cash). */
    public WalletView sandboxTopUp(AuthUser me, BigDecimal amount) {
        me.require(Role.CUSTOMER);
        if (!sandbox) {
            throw ApiException.notFound("NOT_FOUND", "Not found");
        }
        if (amount == null || amount.signum() <= 0 || amount.compareTo(MAX_SANDBOX_TOP_UP) > 0) {
            throw ApiException.badRequest("INVALID_AMOUNT", "Between 1 and 1000");
        }
        wallet.add(me.id(), amount, "TOP_UP", null);
        return view(me);
    }

    @EventListener
    public void onDelivered(OrderChanged change) {
        if (change.to() != OrderStatus.DELIVERED) {
            return;
        }
        orders.findById(change.orderId()).ifPresent(o -> {
            int points = o.total().divide(BigDecimal.TEN, 0, java.math.RoundingMode.DOWN).intValue();
            if (points > 0) {
                loyalty.earnOnce(o.customerId(), points, o.id());
            }
        });
    }
}
