package com.velox.operation.service;

import java.time.Clock;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.velox.common.error.ApiException;
import com.velox.operation.dto.OperationDtos.AdminStats;
import com.velox.operation.repository.DriverRepository;
import com.velox.operation.repository.FeedbackRepository;
import com.velox.operation.repository.OperationsRepository;
import com.velox.order.domain.Order;
import com.velox.order.domain.OrderStatus;
import com.velox.order.dto.OrderDtos.OrderView;
import com.velox.order.repository.OrderRepository;
import com.velox.order.service.OrderLifecycle;
import com.velox.order.service.OrderViews;
import com.velox.security.AuditLog;
import com.velox.security.AuthUser;
import com.velox.security.Role;

/** The admin dashboard (FR-OPS-10, -11): every order, cancelling any order, and today's numbers. Audited. */
@Service
public class AdminOperationsService {

    private final OperationsRepository ops;
    private final OrderRepository orders;
    private final DriverRepository drivers;
    private final FeedbackRepository feedback;
    private final OrderLifecycle lifecycle;
    private final OrderViews views;
    private final AuditLog audit;
    private final Clock clock;

    public AdminOperationsService(OperationsRepository ops, OrderRepository orders, DriverRepository drivers,
                                  FeedbackRepository feedback, OrderLifecycle lifecycle, OrderViews views, AuditLog audit,
                                  Clock clock) {
        this.ops = ops;
        this.orders = orders;
        this.drivers = drivers;
        this.feedback = feedback;
        this.lifecycle = lifecycle;
        this.views = views;
        this.audit = audit;
        this.clock = clock;
    }

    public List<OrderView> orders(AuthUser admin, String status) {
        admin.require(Role.ADMIN);
        String s = null;
        if (status != null && !status.isBlank()) {
            try {
                s = OrderStatus.valueOf(status.trim().toUpperCase(Locale.ROOT)).name();
            } catch (IllegalArgumentException e) {
                throw ApiException.badRequest("INVALID_REQUEST", "Unknown status");
            }
        }
        return ops.allOrders(s).stream().map(id -> views.of(orders.findById(id).orElseThrow())).toList();
    }

    @Transactional
    public OrderView cancel(AuthUser admin, String code, String reason) {
        admin.require(Role.ADMIN);
        Order o = orders.findByCode(code == null ? "" : code.trim().toUpperCase())
                .orElseThrow(() -> ApiException.notFound("ORDER_NOT_FOUND", "Order not found"));
        Order cancelled = lifecycle.move(o.id(), OrderStatus.CANCELLED, admin, reason.trim());
        audit.record(admin, "ORDER_CANCEL", "order", o.code(), reason.trim());
        return views.of(cancelled);
    }

    public AdminStats stats(AuthUser admin) {
        admin.require(Role.ADMIN);
        OperationsRepository.Totals t = ops.totals(null, Today.start(clock));
        return new AdminStats(t.orders(), t.delivered(), t.cancelled(), t.sales(), t.commission(), drivers.online().size(),
                feedback.openComplaints());
    }
}
