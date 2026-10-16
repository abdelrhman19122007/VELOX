package com.velox.operation.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.velox.common.error.ApiException;
import com.velox.operation.dto.OperationDtos.DriverView;
import com.velox.operation.dto.OperationDtos.Earnings;
import com.velox.operation.repository.DriverRepository;
import com.velox.operation.repository.DriverRepository.DriverState;
import com.velox.operation.repository.OperationsRepository;
import com.velox.order.domain.Order;
import com.velox.order.domain.OrderKind;
import com.velox.order.domain.OrderStatus;
import com.velox.order.dto.OrderDtos.OrderView;
import com.velox.order.repository.OrderRepository;
import com.velox.order.service.OrderLifecycle;
import com.velox.order.service.OrderViews;
import com.velox.security.AuthUser;
import com.velox.security.Role;

/**
 * The driver app (FR-OPS-05…09): go online, see offers, take one order at a time, pick it up, deliver it,
 * share the location and see today's earnings. Errands ("bring me anything") have no store: taking one accepts
 * it, and "picked up" means the driver has bought the things.
 */
@Service
public class DriverService {

    /** Sent when a driver takes an order, so it disappears from the other drivers' offers. */
    public record OfferTaken(String orderCode) {
    }

    private final DriverRepository drivers;
    private final OperationsRepository ops;
    private final OrderRepository orders;
    private final OrderLifecycle lifecycle;
    private final OrderViews views;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public DriverService(DriverRepository drivers, OperationsRepository ops, OrderRepository orders, OrderLifecycle lifecycle,
                         OrderViews views, ApplicationEventPublisher events, Clock clock) {
        this.drivers = drivers;
        this.ops = ops;
        this.orders = orders;
        this.lifecycle = lifecycle;
        this.views = views;
        this.events = events;
        this.clock = clock;
    }

    public DriverView status(AuthUser me) {
        me.require(Role.DRIVER);
        DriverState s = drivers.find(me.id()).orElse(new DriverState(false, "MOTORBIKE", null, null));
        return new DriverView(s.online(), s.vehicle(), s.lat(), s.lng());
    }

    public DriverView setStatus(AuthUser me, boolean online, String vehicle) {
        me.require(Role.DRIVER);
        drivers.setStatus(me.id(), online, vehicle);
        return status(me);
    }

    public void location(AuthUser me, double lat, double lng) {
        me.require(Role.DRIVER);
        if (!drivers.setLocation(me.id(), lat, lng)) {
            throw ApiException.conflict("NOT_ONLINE", "Go online first");
        }
    }

    public List<OrderView> offers(AuthUser me) {
        DriverState s = online(me);
        return ops.offers(s.carriesLarge()).stream().map(id -> views.of(orders.findById(id).orElseThrow())).toList();
    }

    @Transactional
    public OrderView take(AuthUser me, String code) {
        DriverState s = online(me);
        if (!ops.driverOrders(me.id(), true).isEmpty()) {
            throw ApiException.conflict("BUSY", "Deliver your current order first");
        }
        Order o = find(code);
        if (!s.carriesLarge() && ops.hasLargeItems(o.id())) {
            throw ApiException.conflict("NEEDS_TUKTUK", "This order has large items — a tuk-tuk or car is needed");
        }
        if (!ops.assign(o.id(), me.id())) {
            throw ApiException.conflict("ALREADY_TAKEN", "Another driver took this order");
        }
        if (o.kind() == OrderKind.ERRAND) {
            lifecycle.move(o.id(), OrderStatus.ACCEPTED, null, null);
        }
        events.publishEvent(new OfferTaken(o.code()));
        return views.of(orders.findById(o.id()).orElseThrow());
    }

    @Transactional
    public OrderView pickUp(AuthUser me, String code) {
        Order o = mine(me, code);
        if (o.kind() == OrderKind.ERRAND && o.status() == OrderStatus.ACCEPTED) {
            lifecycle.move(o.id(), OrderStatus.READY, null, null);
        }
        return views.of(lifecycle.move(o.id(), OrderStatus.PICKED_UP, me, null));
    }

    public OrderView deliver(AuthUser me, String code) {
        return views.of(lifecycle.move(mine(me, code).id(), OrderStatus.DELIVERED, me, null));
    }

    public List<OrderView> myOrders(AuthUser me, boolean active) {
        me.require(Role.DRIVER);
        return ops.driverOrders(me.id(), active).stream().map(id -> views.of(orders.findById(id).orElseThrow())).toList();
    }

    public Earnings earnings(AuthUser me) {
        me.require(Role.DRIVER);
        Object[] e = ops.driverEarnings(me.id(), Today.start(clock));
        return new Earnings((Integer) e[0], (BigDecimal) e[1]);
    }

    private DriverState online(AuthUser me) {
        me.require(Role.DRIVER);
        return drivers.find(me.id()).filter(DriverState::online)
                .orElseThrow(() -> ApiException.conflict("NOT_ONLINE", "Go online first"));
    }

    private Order find(String code) {
        return orders.findByCode(code == null ? "" : code.trim().toUpperCase())
                .orElseThrow(() -> ApiException.notFound("ORDER_NOT_FOUND", "Order not found"));
    }

    /** Only the driver the order is assigned to; for anyone else it doesn't exist. */
    private Order mine(AuthUser me, String code) {
        me.require(Role.DRIVER);
        Order o = find(code);
        if (o.driverId() == null || o.driverId() != me.id()) {
            throw ApiException.notFound("ORDER_NOT_FOUND", "Order not found");
        }
        return o;
    }
}
