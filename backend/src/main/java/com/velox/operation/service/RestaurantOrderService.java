package com.velox.operation.service;

import java.time.Clock;
import java.util.List;

import org.springframework.stereotype.Service;

import com.velox.common.error.ApiException;
import com.velox.operation.dto.OperationDtos.StoreStats;
import com.velox.operation.repository.FeedbackRepository;
import com.velox.operation.repository.FeedbackRepository.Review;
import com.velox.operation.repository.OperationsRepository;
import com.velox.order.domain.Order;
import com.velox.order.domain.OrderStatus;
import com.velox.order.dto.OrderDtos.OrderView;
import com.velox.order.repository.OrderRepository;
import com.velox.order.service.OrderLifecycle;
import com.velox.order.service.OrderViews;
import com.velox.security.AuthUser;
import com.velox.security.Role;

/**
 * The store dashboard (FR-OPS-01…04): its orders, accept / reject / ready, today's numbers and its reviews.
 * A store only ever sees its own paid orders; anything else is "not found".
 */
@Service
public class RestaurantOrderService {

    private final OperationsRepository ops;
    private final OrderRepository orders;
    private final FeedbackRepository feedback;
    private final OrderLifecycle lifecycle;
    private final OrderViews views;
    private final Clock clock;

    public RestaurantOrderService(OperationsRepository ops, OrderRepository orders, FeedbackRepository feedback,
                                  OrderLifecycle lifecycle, OrderViews views, Clock clock) {
        this.ops = ops;
        this.orders = orders;
        this.feedback = feedback;
        this.lifecycle = lifecycle;
        this.views = views;
        this.clock = clock;
    }

    public List<OrderView> orders(AuthUser me, boolean active) {
        me.require(Role.RESTAURANT);
        return ops.storeOrders(me.storeId(), active).stream().map(id -> views.of(orders.findById(id).orElseThrow())).toList();
    }

    public OrderView accept(AuthUser me, String code) {
        return views.of(lifecycle.move(own(me, code).id(), OrderStatus.ACCEPTED, me, null));
    }

    public OrderView reject(AuthUser me, String code, String reason) {
        return views.of(lifecycle.move(own(me, code).id(), OrderStatus.REJECTED, me, reason.trim()));
    }

    public OrderView ready(AuthUser me, String code) {
        return views.of(lifecycle.move(own(me, code).id(), OrderStatus.READY, me, null));
    }

    public StoreStats stats(AuthUser me) {
        me.require(Role.RESTAURANT);
        OperationsRepository.Totals t = ops.totals(me.storeId(), Today.start(clock));
        return new StoreStats(t.orders(), t.delivered(), t.cancelled(), t.sales());
    }

    public List<Review> reviews(AuthUser me) {
        me.require(Role.RESTAURANT);
        return feedback.storeReviews(me.storeId());
    }

    private Order own(AuthUser me, String code) {
        me.require(Role.RESTAURANT);
        return orders.findByCode(code == null ? "" : code.trim().toUpperCase())
                .filter(o -> me.storeId() != null && me.storeId().equals(o.storeId()) && o.status() != OrderStatus.PENDING_PAYMENT)
                .orElseThrow(() -> ApiException.notFound("ORDER_NOT_FOUND", "Order not found"));
    }
}
