package com.velox.order.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.velox.common.error.ApiException;
import com.velox.order.domain.Order;
import com.velox.order.domain.OrderKind;
import com.velox.order.domain.OrderStatus;
import com.velox.order.domain.PaymentMethod;
import com.velox.order.domain.PaymentStatus;
import com.velox.order.dto.OrderDtos.OrderView;
import com.velox.order.dto.OrderDtos.PlaceOrderRequest;
import com.velox.order.dto.OrderDtos.Quote;
import com.velox.order.dto.OrderDtos.QuoteRequest;
import com.velox.order.repository.OrderRepository;
import com.velox.order.service.PricingService.Priced;
import com.velox.order.spi.DiscountPolicy;
import com.velox.order.spi.PaymentStarter;
import com.velox.security.AuthUser;
import com.velox.security.Role;

/** The customer side of orders: quote, place, list, view, cancel, return (FR-ORD-01…13). */
@Service
public class OrderService {

    static final Duration RETURN_WINDOW = Duration.ofDays(1);
    static final Duration MAX_SCHEDULE_AHEAD = Duration.ofDays(7);

    private final PricingService pricing;
    private final OrderRepository orders;
    private final OrderLifecycle lifecycle;
    private final OrderViews views;
    private final List<PaymentStarter> payments;
    private final Optional<DiscountPolicy> discounts;
    private final Clock clock;

    public OrderService(PricingService pricing, OrderRepository orders, OrderLifecycle lifecycle, OrderViews views,
                        List<PaymentStarter> payments, Optional<DiscountPolicy> discounts, Clock clock) {
        this.pricing = pricing;
        this.orders = orders;
        this.lifecycle = lifecycle;
        this.views = views;
        this.payments = payments;
        this.discounts = discounts;
        this.clock = clock;
    }

    public Quote quote(AuthUser customer, QuoteRequest r) {
        return pricing.price(customer, kind(r.kind()), r.zone(), r.items(), r.promo()).toQuote();
    }

    @Transactional
    public OrderView place(AuthUser customer, PlaceOrderRequest r) {
        customer.require(Role.CUSTOMER);
        OrderKind kind = kind(r.kind());
        if (kind == OrderKind.ERRAND && (r.errandDetails() == null || r.errandDetails().isBlank())) {
            throw ApiException.badRequest("INVALID_ERRAND", "Write what the driver should buy");
        }
        validateSchedule(r.scheduledFor());
        Priced priced = pricing.price(customer, kind, r.zone(), r.items(), r.promo());
        PaymentMethod method = r.paymentMethod() == null ? PaymentMethod.CASH : r.paymentMethod();
        PaymentStarter starter = null;
        if (method.isOnline()) {
            starter = payments.stream().filter(p -> p.supports(method)).findFirst()
                    .orElseThrow(() -> ApiException.badRequest("PAYMENTS_UNAVAILABLE", "This payment method isn't available"));
        }
        if (r.cashChangeFor() != null && (method.isOnline() || r.cashChangeFor().compareTo(priced.total()) < 0)) {
            throw ApiException.badRequest("INVALID_CHANGE", "The cash note must cover the total");
        }
        // online orders wait, unseen by the store, until they're paid
        OrderStatus first = method.isOnline() ? OrderStatus.PENDING_PAYMENT : OrderStatus.PLACED;
        Order draft = new Order(0, orders.nextCode(), kind, customer.id(), priced.storeId(), null, first, priced.zone().code(),
                r.phone().trim(), r.address().trim(), blank(r.landmark()), r.lat(), r.lng(),
                kind == OrderKind.ERRAND ? r.errandDetails().trim() : null, priced.subtotal(), priced.discount(),
                priced.deliveryFee(), priced.total(), r.cashChangeFor(), method,
                method.isOnline() ? PaymentStatus.PENDING : PaymentStatus.COD,
                r.promo() == null || r.promo().isBlank() ? null : r.promo().trim().toUpperCase(), null, r.scheduledFor(), null, null);
        long id = orders.insert(draft, priced.items());
        Order saved = orders.findById(id).orElseThrow();
        if (draft.promo() != null) {
            discounts.ifPresent(d -> d.consume(customer, draft.promo(), id));
        }
        lifecycle.created(id, first, customer);
        if (starter != null) {
            starter.start(saved, r.walletPhone());
            saved = orders.findById(saved.id()).orElseThrow();   // a wallet payment is already PAID + PLACED
        }
        return views.of(saved);
    }

    public List<OrderView> mine(AuthUser customer) {
        return orders.findByCustomer(customer.id(), 50).stream().map(views::of).toList();
    }

    /** Visible only to its customer, its store's staff, its driver and admins — anyone else gets 404 (no IDOR). */
    public OrderView get(AuthUser user, String code) {
        return views.of(visible(user, code));
    }

    /** The customer can cancel until the store accepts (FR-ORD-09). */
    @Transactional
    public OrderView cancel(AuthUser customer, String code) {
        Order o = own(customer, code);
        if (o.status() != OrderStatus.PLACED && o.status() != OrderStatus.PENDING_PAYMENT) {
            throw ApiException.conflict("CANNOT_CANCEL", "The store already accepted this order");
        }
        return views.of(lifecycle.move(o.id(), OrderStatus.CANCELLED, customer, "Cancelled by the customer"));
    }

    /** A delivered order can be returned within one day (FR-ORD-12); refunds are done by the payments module. */
    @Transactional
    public OrderView returnOrder(AuthUser customer, String code) {
        Order o = own(customer, code);
        if (o.status() != OrderStatus.DELIVERED || o.updatedAt().plus(RETURN_WINDOW).isBefore(clock.instant())) {
            throw ApiException.conflict("RETURN_EXPIRED", "Returns are possible within one day of delivery");
        }
        return views.of(lifecycle.move(o.id(), OrderStatus.RETURNED, customer, null));
    }

    Order visible(AuthUser user, String code) {
        Order o = orders.findByCode(code == null ? "" : code.trim().toUpperCase())
                .orElseThrow(() -> ApiException.notFound("ORDER_NOT_FOUND", "Order not found"));
        boolean allowed = switch (user.role()) {
            case ADMIN -> true;
            case CUSTOMER -> o.customerId() == user.id();
            case RESTAURANT -> user.storeId() != null && user.storeId().equals(o.storeId()) && o.status() != OrderStatus.PENDING_PAYMENT;
            case DRIVER -> o.driverId() != null && o.driverId() == user.id();
        };
        if (!allowed) {
            throw ApiException.notFound("ORDER_NOT_FOUND", "Order not found");
        }
        return o;
    }

    private Order own(AuthUser customer, String code) {
        customer.require(Role.CUSTOMER);
        return visible(customer, code);
    }

    private void validateSchedule(Instant when) {
        if (when == null) {
            return;
        }
        Instant now = clock.instant();
        if (when.isBefore(now.plus(Duration.ofMinutes(30))) || when.isAfter(now.plus(MAX_SCHEDULE_AHEAD))) {
            throw ApiException.badRequest("INVALID_SCHEDULE", "Schedule between 30 minutes and 7 days ahead");
        }
    }

    private static OrderKind kind(OrderKind k) {
        return k == null ? OrderKind.STORE : k;
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    static BigDecimal orZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
