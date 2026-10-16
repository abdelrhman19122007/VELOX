package com.velox.order.service;

import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.velox.common.error.ApiException;
import com.velox.order.domain.Order;
import com.velox.order.domain.OrderStatus;
import com.velox.order.domain.OrderStatus.Actor;
import com.velox.order.domain.PaymentStatus;
import com.velox.order.repository.OrderRepository;
import com.velox.order.spi.OrderChanged;
import com.velox.security.AuthUser;
import com.velox.security.Role;

/**
 * The only way an order changes status (FR-ORD-09). Checks the State-machine rule ({@link OrderStatus}) AND that
 * the account is related to this order (its customer, its store's staff, its assigned driver), records the step in
 * the timeline and tells listeners ({@link OrderChanged}). Restaurant / driver / payment modules all call this.
 */
@Service
public class OrderLifecycle {

    private final OrderRepository orders;
    private final ApplicationEventPublisher events;

    public OrderLifecycle(OrderRepository orders, ApplicationEventPublisher events) {
        this.orders = orders;
        this.events = events;
    }

    /** {@code actor} null = the system (timeouts, payment confirmation). Returns the updated order. */
    @Transactional
    public Order move(long orderId, OrderStatus to, AuthUser actor, String reason) {
        Order order = orders.findById(orderId).orElseThrow(() -> ApiException.notFound("ORDER_NOT_FOUND", "Order not found"));
        requireRelated(order, actor);
        OrderStatus from = order.status();
        if (!from.canMoveTo(to, Actor.of(actor == null ? null : actor.role()))) {
            throw new ApiException(HttpStatus.CONFLICT, "BAD_STATUS", "The order is " + from, Map.of("status", from.name()));
        }
        if (!orders.moveStatus(orderId, from, to, reason)) {
            Order now = orders.findById(orderId).orElseThrow();
            throw new ApiException(HttpStatus.CONFLICT, "BAD_STATUS", "The order is " + now.status(),
                    Map.of("status", now.status().name()));
        }
        orders.addEvent(orderId, to, actor == null ? null : actor.id());
        events.publishEvent(new OrderChanged(orderId, from, to));
        return orders.findById(orderId).orElseThrow();
    }

    /** Called by the payments module when an online payment succeeds: the order goes to the store. */
    @Transactional
    public Order markPaid(long orderId) {
        orders.setPaymentStatus(orderId, PaymentStatus.PENDING, PaymentStatus.PAID);
        return move(orderId, OrderStatus.PLACED, null, null);
    }

    /** Records the first status of a new order and announces it. */
    void created(long orderId, OrderStatus status, AuthUser customer) {
        orders.addEvent(orderId, status, customer.id());
        events.publishEvent(new OrderChanged(orderId, null, status));
    }

    /** A store can only move its own orders, a driver only the order assigned to them, a customer only theirs. */
    private static void requireRelated(Order order, AuthUser actor) {
        if (actor == null || actor.is(Role.ADMIN)) {
            return;
        }
        boolean related = switch (actor.role()) {
            case CUSTOMER -> actor.id() == order.customerId();
            case RESTAURANT -> actor.storeId() != null && actor.storeId().equals(order.storeId());
            case DRIVER -> order.driverId() != null && order.driverId() == actor.id();
            default -> false;
        };
        if (!related) {
            throw ApiException.notFound("ORDER_NOT_FOUND", "Order not found");
        }
    }
}
