package com.velox.order.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.velox.order.domain.Order;
import com.velox.order.dto.OrderDtos.DriverRef;
import com.velox.order.dto.OrderDtos.ItemView;
import com.velox.order.dto.OrderDtos.OrderView;
import com.velox.order.dto.OrderDtos.StepView;
import com.velox.order.dto.OrderDtos.StoreRef;
import com.velox.order.repository.OrderRepository;

/** Builds the full view of an order: items, store, driver (with live position), timeline. */
@Component
public class OrderViews {

    private final OrderRepository orders;
    private final JdbcTemplate db;

    public OrderViews(OrderRepository orders, JdbcTemplate db) {
        this.orders = orders;
        this.db = db;
    }

    public OrderView of(Order o) {
        StoreRef store = o.storeId() == null ? null : db.query("SELECT id, name, name_ar, lat, lng FROM stores WHERE id = ?",
                rs -> rs.next() ? new StoreRef(rs.getString(1), rs.getString(2), rs.getString(3),
                        (Double) rs.getObject(4, Double.class), (Double) rs.getObject(5, Double.class)) : null, o.storeId());
        DriverRef driver = o.driverId() == null ? null : db.query("SELECT u.name, u.phone, d.lat, d.lng FROM users u "
                + "LEFT JOIN driver_status d ON d.user_id = u.id WHERE u.id = ?",
                rs -> rs.next() ? new DriverRef(rs.getString(1), rs.getString(2), (Double) rs.getObject(3, Double.class),
                        (Double) rs.getObject(4, Double.class)) : null, o.driverId());
        String customer = db.queryForObject("SELECT name FROM users WHERE id = ?", String.class, o.customerId());
        return new OrderView(o.code(), o.kind(), o.status(), store, o.zone(), o.phone(), o.address(), o.landmark(), o.lat(),
                o.lng(), o.errandDetails(), orders.items(o.id()).stream().map(ItemView::of).toList(), o.subtotal(),
                o.discount(), o.deliveryFee(), o.total(), o.cashChangeFor(), o.paymentMethod(), o.paymentStatus(),
                o.rejectReason(), o.scheduledFor(), o.createdAt(), customer, driver,
                orders.timeline(o.id()).stream().map(s -> new StepView(s.status(), s.at())).toList());
    }
}
